package org.nep.nepsystem;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.junit.jupiter.api.Test;
import org.nep.nepsystem.bean.Alerts;
import org.nep.nepsystem.bean.Devices;
import org.nep.nepsystem.dao.AlertsDao;
import org.nep.nepsystem.dao.DevicesDao;
import org.nep.nepsystem.dto.DeviceSituationDTO;
import org.nep.nepsystem.dto.SituationOverviewDTO;
import org.nep.nepsystem.service.MonitorDataService;
import org.nep.nepsystem.service.SituationService;
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
 * 空间态势测试（空间态势升级）：
 * 专用测试设备：正常/预警(WARN 活跃告警)/报警(ALARM 活跃告警)/离线。
 */
@SpringBootTest(properties = {"simulator.enabled=false", "quality.scan.enabled=false", "alert.auto-resolve-hold-ms=0"})
@Transactional
@AutoConfigureMockMvc
class SituationTests {

    @Autowired private SituationService situationService;
    @Autowired private DevicesDao devicesDao;
    @Autowired private AlertsDao alertsDao;
    @Autowired private MonitorDataService monitorDataService;
    @Autowired private MockMvc mockMvc;

    private Devices insertDevice(String code, String type, int status) {
        Devices d = new Devices();
        d.setDeviceCode(code);
        d.setDeviceName("态势测试-" + code);
        d.setType(type);
        d.setLocation("测试位置-" + code);
        d.setStatus(status);
        d.setLastReportTime(new Date());
        devicesDao.insert(d);
        return d;
    }

    private void insertActiveAlert(Devices d, String level, String sensorCode) {
        Alerts a = new Alerts();
        a.setDeviceId(d.getId());
        a.setSensorCode(sensorCode);
        a.setLevel(level);
        a.setState(level);
        a.setAlertValue(new BigDecimal("88.00"));
        a.setStatus(0);
        a.setCreateTime(new Date());
        alertsDao.insert(a);
    }

    private void report(Devices d, String sensorCode, BigDecimal value) {
        Map<String, Object> item = new HashMap<>();
        item.put("sensorCode", sensorCode);
        item.put("value", value);
        monitorDataService.report(d.getDeviceCode(), Collections.singletonList(item), new Date());
    }

    @Test
    void overviewClassifiesStates() {
        Devices normal = insertDevice("E2E-SIT-NORMAL", "AIR", 1);
        Devices warn = insertDevice("E2E-SIT-WARN", "WATER", 1);
        Devices alarm = insertDevice("E2E-SIT-ALARM", "NOISE", 1);
        Devices offline = insertDevice("E2E-SIT-OFF", "AIR", 0);
        insertActiveAlert(warn, "WARN", "TURBIDITY");
        insertActiveAlert(alarm, "ALARM", "NOISE");
        insertActiveAlert(alarm, "WARN", "NOISE"); // 报警设备同时有预警

        SituationOverviewDTO o = situationService.overview();
        assertTrue(o.getTotalDevices() >= 4);
        assertTrue(o.getOnlineDevices() >= 3);
        assertTrue(o.getOfflineDevices() >= 1);
        assertTrue(o.getWarnDevices() >= 1, "至少 1 台预警设备");
        assertTrue(o.getAlarmDevices() >= 1, "至少 1 台报警设备");
        assertTrue(o.getActiveAlerts() >= 3, "3 条活跃告警");
        assertFalse(o.getHealthy(), "存在活跃告警时环境状态非良好");
    }

    @Test
    void healthyWhenNoActiveAlerts() {
        Devices normal = insertDevice("E2E-SIT-HEALTHY", "AIR", 1);
        report(normal, "TEMP", new BigDecimal("25.00"));
        SituationOverviewDTO o = situationService.overview();
        // 本测试事务内无活跃告警（共享库当前亦为 0 活跃）
        assertEquals(true, o.getHealthy(), "无活跃告警时应为环境状态良好");
    }

    @Test
    void devicesListWithFiltersAndDetail() {
        Devices normal = insertDevice("E2E-SIT-LIST-N", "AIR", 1);
        Devices warn = insertDevice("E2E-SIT-LIST-W", "WATER", 1);
        Devices offline = insertDevice("E2E-SIT-LIST-O", "NOISE", 0);
        report(normal, "TEMP", new BigDecimal("25.00"));
        insertActiveAlert(warn, "WARN", "TURBIDITY");

        // 全部
        List<DeviceSituationDTO> all = situationService.devices(null, null, null, null);
        DeviceSituationDTO w = all.stream().filter(d -> "E2E-SIT-LIST-W".equals(d.getDeviceCode())).findFirst().orElse(null);
        assertNotNull(w);
        assertEquals(true, w.getWarn(), "有 WARN 活跃告警");
        assertEquals(false, w.getAlarm());
        assertEquals("测试位置-E2E-SIT-LIST-W", w.getLocation());
        assertNotNull(w.getValues());
        assertFalse(w.getAlerts().isEmpty());

        // 按类型过滤（种子库仅有 1 台 AIR，断言全部匹配即可）
        List<DeviceSituationDTO> airs = situationService.devices(null, "AIR", null, null);
        assertFalse(airs.isEmpty());
        assertTrue(airs.stream().allMatch(d -> "AIR".equals(d.getType())));
        List<DeviceSituationDTO> waters = situationService.devices(null, "WATER", null, null);
        assertTrue(waters.stream().allMatch(d -> "WATER".equals(d.getType())));

        // 按状态过滤
        List<DeviceSituationDTO> offs = situationService.devices(null, null, 0, null);
        assertTrue(offs.stream().allMatch(d -> d.getStatus() == 0));
        // 按告警过滤
        List<DeviceSituationDTO> warns = situationService.devices(null, null, null, "WARN");
        assertTrue(warns.stream().allMatch(d -> Boolean.TRUE.equals(d.getWarn())));
        List<DeviceSituationDTO> nones = situationService.devices(null, null, null, "NONE");
        assertTrue(nones.stream().noneMatch(d -> Boolean.TRUE.equals(d.getWarn()) || Boolean.TRUE.equals(d.getAlarm())));
        // 关键字过滤
        List<DeviceSituationDTO> kw = situationService.devices("SIT-LIST-N", null, null, null);
        assertEquals(1, kw.size());
        // 健康度字段
        assertNotNull(w.getHealthScore());
        assertNotNull(w.getHealthLevel());
        assertNotNull(w.getQualityStatus());
    }

    @Test
    void situationEndpointsWork() throws Exception {
        Devices d = insertDevice("E2E-SIT-EP", "AIR", 1);
        insertActiveAlert(d, "ALARM", "PM25");
        mockMvc.perform(get("/api/situation/overview"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.healthy").value(false));
        mockMvc.perform(get("/api/situation/devices").param("alertLevel", "ALARM"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }
}
