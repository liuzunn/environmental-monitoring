package org.nep.nepsystem;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import org.junit.jupiter.api.Test;
import org.nep.nepsystem.bean.Alerts;
import org.nep.nepsystem.bean.DataQuality;
import org.nep.nepsystem.bean.Devices;
import org.nep.nepsystem.dao.AlertsDao;
import org.nep.nepsystem.dao.DataQualityDao;
import org.nep.nepsystem.dao.DevicesDao;
import org.nep.nepsystem.dto.DeviceHealthDTO;
import org.nep.nepsystem.service.DeviceHealthService;
import org.nep.nepsystem.service.MonitorDataService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 设备健康度测试（业务层升级模块四）：
 * 使用全新测试设备（E2E-HEALTH-*）保证计算确定性（不受历史数据影响）。
 */
@SpringBootTest(properties = {"simulator.enabled=false", "quality.scan.enabled=false", "alert.auto-resolve-hold-ms=0"})
@Transactional
@AutoConfigureMockMvc
class DeviceHealthTests {

    @Autowired private MonitorDataService monitorDataService;
    @Autowired private DeviceHealthService deviceHealthService;
    @Autowired private DevicesDao devicesDao;
    @Autowired private DataQualityDao dataQualityDao;
    @Autowired private AlertsDao alertsDao;
    @Autowired private MockMvc mockMvc;

    private Devices insertDevice(String code, String name, String type) {
        Devices d = new Devices();
        d.setDeviceCode(code);
        d.setDeviceName(name);
        d.setType(type);
        d.setStatus(1);
        d.setLastReportTime(new Date());
        devicesDao.insert(d);
        return d;
    }

    private void insertAnomaly(Devices d, String issueType) {
        DataQuality dq = new DataQuality();
        dq.setDeviceId(d.getId());
        dq.setSensorCode("TEMP");
        dq.setCategory("ANOMALY");
        dq.setIssueType(issueType);
        dq.setSeverity("CRITICAL");
        dq.setDetail("test");
        dq.setFirstSeen(new Date());
        dq.setLastSeen(new Date());
        dq.setOccurrenceCount(2);
        dataQualityDao.insert(dq);
    }

    private void insertAlert(Devices d) {
        Alerts a = new Alerts();
        a.setDeviceId(d.getId());
        a.setSensorCode("TEMP");
        a.setLevel("WARN");
        a.setState("WARN");
        a.setAlertValue(new BigDecimal("30"));
        a.setStatus(0);
        a.setCreateTime(new Date());
        alertsDao.insert(a);
    }

    @Test
    void freshDeviceWithPartialData() {
        Devices dev = insertDevice("E2E-HEALTH-AIR", "健康测试空气站", "AIR");
        // 只上报 TEMP 一条（AIR 应上报 4 个指标 -> 完整率 25%）
        Map<String, Object> item = new HashMap<>();
        item.put("sensorCode", "TEMP");
        item.put("value", new BigDecimal("25.00"));
        monitorDataService.report("E2E-HEALTH-AIR", Collections.singletonList(item), new Date());
        // 制造 1 类异常(2次) + 1 条告警
        insertAnomaly(dev, "ZSCORE");
        insertAlert(dev);

        DeviceHealthDTO dto = deviceHealthService.calculate(dev.getId());
        assertNotNull(dto);
        assertTrue(dto.getOnlineRate() > 0, "近7天有数据，在线率应 > 0");
        assertNotNull(dto.getLastCommunication());
        assertEquals(25.0, dto.getDataCompleteness(), 1e-6, "AIR 上报 1/4 指标，完整率 25%");
        assertEquals(2L, dto.getAnomalyCount(), "异常累计触发 2 次");
        assertEquals(1L, dto.getAlertCount(), "近 7 天 1 条告警");
        assertNotNull(dto.getHealthScore());
        assertTrue(dto.getHealthScore() >= 0 && dto.getHealthScore() <= 100);
        assertNotNull(dto.getHealthLevel());
        assertTrue(Arrays.asList("HEALTHY", "FAIR", "POOR").contains(dto.getHealthLevel()));
    }

    @Test
    void neverReportedDeviceIsPoor() {
        Devices dev = insertDevice("E2E-HEALTH-NONE", "从未上报设备", "NOISE");
        // 清空最近通信时间模拟从未上报（updateById 忽略 null，须用 UpdateWrapper 显式置空）
        devicesDao.update(null, new UpdateWrapper<Devices>().eq("id", dev.getId()).set("last_report_time", null));

        DeviceHealthDTO dto = deviceHealthService.calculate(dev.getId());
        assertNotNull(dto);
        assertEquals(0.0, dto.getOnlineRate(), 1e-6);
        assertNull(dto.getLastCommunication());
        assertEquals(0.0, dto.getDataCompleteness(), 1e-6);
        assertTrue(dto.getHealthScore() < 40, "无任何数据健康分应很低，实际 " + dto.getHealthScore());
        assertEquals("POOR", dto.getHealthLevel());
    }

    @Test
    void healthEndpointsWork() throws Exception {
        Devices dev = insertDevice("E2E-HEALTH-EP", "接口测试设备", "AIR");
        mockMvc.perform(get("/api/health/devices"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
        mockMvc.perform(get("/api/health/devices/" + dev.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.deviceId").value(dev.getId()));
        mockMvc.perform(get("/api/health/devices/999999"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400));
    }
}
