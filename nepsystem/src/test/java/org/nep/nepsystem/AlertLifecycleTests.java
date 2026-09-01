package org.nep.nepsystem;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import org.junit.jupiter.api.Test;
import org.nep.nepsystem.bean.Alerts;
import org.nep.nepsystem.bean.Thresholds;
import org.nep.nepsystem.dao.AlertsDao;
import org.nep.nepsystem.dao.ThresholdsDao;
import org.nep.nepsystem.service.AlertLifecycleService;
import org.nep.nepsystem.service.MonitorDataService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 告警生命周期升级测试（业务层升级模块一）：
 * 1. 完整状态机 WARN/ALARM -> ACKNOWLEDGED -> PROCESSING -> RESOLVED
 * 2. 自动恢复 NORMAL
 * 3. 旧接口 /handle 兼容（等价解决）
 * 4. 新接口 /acknowledge /process /resolve
 * 5. page 新增 state 过滤
 * 说明：@Transactional 保证测试数据自动回滚；auto-resolve-hold-ms=0 便于测试自动恢复。
 */
@SpringBootTest(properties = {"simulator.enabled=false", "alert.auto-resolve-hold-ms=0"})
@Transactional
@AutoConfigureMockMvc
class AlertLifecycleTests {

    @Autowired
    private MonitorDataService monitorDataService;

    @Autowired
    private ThresholdsDao thresholdsDao;

    @Autowired
    private AlertsDao alertsDao;

    @Autowired
    private AlertLifecycleService alertLifecycleService;

    @Autowired
    private MockMvc mockMvc;

    /** 为 TURBIDITY 建全局阈值：预警 3 / 报警 8（避开种子数据已占用的 PM25/NOISE） */
    private void ensureTurbidityThreshold() {
        Thresholds thr = new Thresholds();
        thr.setDeviceId(null);
        thr.setSensorCode("TURBIDITY");
        thr.setWarnMax(new BigDecimal("3"));
        thr.setAlarmMax(new BigDecimal("8"));
        thr.setEnabled(1);
        thresholdsDao.insert(thr);
    }

    /** 上报超标值并返回生成的告警（ALARM） */
    private Alerts reportAlarm(String deviceCode, BigDecimal value) {
        Map<String, Object> item = new HashMap<>();
        item.put("sensorCode", "TURBIDITY");
        item.put("value", value);
        monitorDataService.report(deviceCode, Collections.singletonList(item), new Date());
        List<Alerts> list = alertsDao.selectList(new QueryWrapper<Alerts>()
                .eq("sensor_code", "TURBIDITY").orderByDesc("id").last("limit 1"));
        assertFalse(list.isEmpty(), "应生成告警");
        return list.get(0);
    }

    @Test
    void fullLifecycleTransitions() {
        ensureTurbidityThreshold();
        Alerts alert = reportAlarm("DEV-WTR-001", new BigDecimal("12.00"));
        assertEquals("ALARM", alert.getLevel());
        assertEquals("ALARM", alert.getState(), "新建告警 state 应为级别");
        assertEquals(0, alert.getStatus());

        // 确认
        Alerts acked = alertLifecycleService.acknowledge(alert.getId(), "tester");
        assertEquals("ACKNOWLEDGED", acked.getState());
        assertEquals(0, acked.getStatus());
        assertNotNull(acked.getAckTime());
        assertEquals("tester", acked.getAckUser());

        // 处理
        Alerts processing = alertLifecycleService.process(alert.getId(), "operator");
        assertEquals("PROCESSING", processing.getState());
        assertEquals(0, processing.getStatus());
        assertNotNull(processing.getHandleTime());
        assertEquals("operator", processing.getHandleUser());

        // 解决
        Alerts resolved = alertLifecycleService.resolve(alert.getId(), "admin");
        assertEquals("RESOLVED", resolved.getState());
        assertEquals(1, resolved.getStatus(), "解决后 status 应为 1（兼容旧语义）");
        assertNotNull(resolved.getResolveTime());
        assertNotNull(resolved.getDurationSeconds());
        assertTrue(resolved.getDurationSeconds() >= 0);
    }

    @Test
    void autoRecoveryToNormal() {
        ensureTurbidityThreshold();
        Alerts alert = reportAlarm("DEV-WTR-001", new BigDecimal("12.00"));
        assertEquals("ALARM", alert.getState());

        // 指标回到正常范围 -> 自动恢复 NORMAL
        Map<String, Object> item = new HashMap<>();
        item.put("sensorCode", "TURBIDITY");
        item.put("value", new BigDecimal("1.00"));
        monitorDataService.report("DEV-WTR-001", Collections.singletonList(item), new Date());

        Alerts after = alertsDao.selectById(alert.getId());
        assertEquals("NORMAL", after.getState(), "指标恢复正常应自动置 NORMAL");
        assertEquals(1, after.getStatus());
        assertEquals("SYSTEM", after.getResolveUser());
        assertNotNull(after.getResolveTime());
    }

    @Test
    void legacyHandleEndpointCompatible() throws Exception {
        ensureTurbidityThreshold();
        Alerts alert = reportAlarm("DEV-WTR-001", new BigDecimal("12.00"));

        mockMvc.perform(put("/api/alerts/" + alert.getId() + "/handle")
                .param("handleUser", "legacy-user"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        Alerts after = alertsDao.selectById(alert.getId());
        assertEquals(1, after.getStatus(), "旧接口处理后 status=1");
        assertEquals("RESOLVED", after.getState(), "旧接口等价于解决");
        assertEquals("legacy-user", after.getResolveUser());
        assertNotNull(after.getResolveTime());
    }

    @Test
    void newLifecycleEndpoints() throws Exception {
        ensureTurbidityThreshold();
        Alerts alert = reportAlarm("DEV-WTR-001", new BigDecimal("12.00"));

        // 预存问题修复：id 以字符串序列化（雪花 ID 超出 JS 安全整数）
        mockMvc.perform(get("/api/alerts/page").param("size", "1"))
                .andExpect(jsonPath("$.data.records[0].id").isString());
        mockMvc.perform(put("/api/alerts/" + alert.getId() + "/acknowledge")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"user\":\"mvc-user\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.state").value("ACKNOWLEDGED"));

        mockMvc.perform(put("/api/alerts/" + alert.getId() + "/process"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.state").value("PROCESSING"));

        mockMvc.perform(put("/api/alerts/" + alert.getId() + "/resolve"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.state").value("RESOLVED"))
                .andExpect(jsonPath("$.data.status").value(1))
                .andExpect(jsonPath("$.data.durationSeconds").isNumber());
    }

    @Test
    void pageStateFilterWorks() throws Exception {
        ensureTurbidityThreshold();
        Alerts alert = reportAlarm("DEV-WTR-001", new BigDecimal("12.00"));
        alertLifecycleService.resolve(alert.getId(), "admin");

        mockMvc.perform(get("/api/alerts/page").param("state", "RESOLVED").param("size", "50"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.total").isNumber());
        // 校验返回中确含刚解决的告警且全部为 RESOLVED
        List<Alerts> rows = alertsDao.selectList(new QueryWrapper<Alerts>()
                .eq("state", "RESOLVED").orderByDesc("id").last("limit 50"));
        assertTrue(rows.stream().anyMatch(a -> a.getId().equals(alert.getId())));
        assertTrue(rows.stream().allMatch(a -> "RESOLVED".equals(a.getState())));
    }
}
