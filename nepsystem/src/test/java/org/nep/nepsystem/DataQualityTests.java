package org.nep.nepsystem;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.junit.jupiter.api.Test;
import org.nep.nepsystem.bean.DataQuality;
import org.nep.nepsystem.bean.Devices;
import org.nep.nepsystem.dao.DataQualityDao;
import org.nep.nepsystem.dao.DevicesDao;
import org.nep.nepsystem.service.DataQualityService;
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
 * 数据质量检测测试（业务层升级模块二）：
 * 每个用例使用独立测试设备（E2E-QUALITY-*），按设备过滤断言，避免共享库中历史记录干扰。
 */
@SpringBootTest(properties = {
        "simulator.enabled=false",
        "quality.scan.enabled=true",
        "quality.offline-threshold-ms=60000",
        "alert.auto-resolve-hold-ms=0"})
@Transactional
@AutoConfigureMockMvc
class DataQualityTests {

    @Autowired private MonitorDataService monitorDataService;
    @Autowired private DataQualityService dataQualityService;
    @Autowired private DataQualityDao dataQualityDao;
    @Autowired private DevicesDao devicesDao;
    @Autowired private MockMvc mockMvc;

    private Devices insertDevice(String code, String type) {
        Devices d = new Devices();
        d.setDeviceCode(code);
        d.setDeviceName("质量测试-" + code);
        d.setType(type);
        d.setStatus(1);
        d.setLastReportTime(new Date());
        devicesDao.insert(d);
        return d;
    }

    private void report(String deviceCode, String sensorCode, BigDecimal value) {
        Map<String, Object> item = new HashMap<>();
        item.put("sensorCode", sensorCode);
        item.put("value", value);
        monitorDataService.report(deviceCode, Collections.singletonList(item), new Date());
    }

    private List<DataQuality> issuesOf(Integer deviceId, String issueType) {
        return dataQualityDao.selectList(new QueryWrapper<DataQuality>()
                .eq("device_id", deviceId).eq("issue_type", issueType)
                .orderByDesc("id").last("limit 1"));
    }

    @Test
    void nullValueDetected() {
        Devices dev = insertDevice("E2E-QUALITY-NULL", "AIR");
        // value 缺失 -> NULL_VALUE
        Map<String, Object> item = new HashMap<>();
        item.put("sensorCode", "PM25");
        monitorDataService.report(dev.getDeviceCode(), Collections.singletonList(item), new Date());

        List<DataQuality> rows = issuesOf(dev.getId(), "NULL_VALUE");
        assertFalse(rows.isEmpty(), "应记录 NULL_VALUE");
        assertEquals("QUALITY", rows.get(0).getCategory());
        assertEquals("PM25", rows.get(0).getSensorCode());
    }

    @Test
    void outOfRangeDetected() {
        Devices dev = insertDevice("E2E-QUALITY-RANGE", "AIR");
        // PM25 量程 [0,500]，9999 超上限
        report(dev.getDeviceCode(), "PM25", new BigDecimal("9999"));
        List<DataQuality> rows = issuesOf(dev.getId(), "OUT_OF_RANGE");
        assertFalse(rows.isEmpty());
        assertEquals("CRITICAL", rows.get(0).getSeverity());
        assertEquals(0, new BigDecimal("9999").compareTo(rows.get(0).getLatestValue()), "存储后为 DECIMAL(10,2)，按数值比较");
    }

    @Test
    void constantValueDetected() {
        Devices dev = insertDevice("E2E-QUALITY-CONST", "AIR");
        // HUMI 连续 6 条相同（默认阈值 5）
        for (int i = 0; i < 6; i++) {
            report(dev.getDeviceCode(), "HUMI", new BigDecimal("50.00"));
        }
        List<DataQuality> rows = issuesOf(dev.getId(), "CONSTANT_VALUE");
        assertFalse(rows.isEmpty(), "应记录 CONSTANT_VALUE");
        assertEquals("WARN", rows.get(0).getSeverity());
    }

    @Test
    void suddenChangeDetected() {
        Devices dev = insertDevice("E2E-QUALITY-JUMP", "AIR");
        // CO2 量程 [300,5000]，跨度 4700，突变阈值 50%=2350；300 -> 4900 差值 4600
        report(dev.getDeviceCode(), "CO2", new BigDecimal("300"));
        report(dev.getDeviceCode(), "CO2", new BigDecimal("4900"));
        List<DataQuality> rows = issuesOf(dev.getId(), "SUDDEN_CHANGE");
        assertFalse(rows.isEmpty(), "应记录 SUDDEN_CHANGE");
        assertEquals("CRITICAL", rows.get(0).getSeverity());
    }

    @Test
    void offlineDetectionMarksDeviceOffline() {
        Devices dev = insertDevice("E2E-QUALITY-OFF", "NOISE");
        // 把最近上报时间拨到 10 分钟前（> offline-threshold 60s）
        Devices upd = new Devices();
        upd.setId(dev.getId());
        upd.setStatus(1);
        upd.setLastReportTime(new Date(System.currentTimeMillis() - 10 * 60 * 1000L));
        devicesDao.updateById(upd);

        dataQualityService.scanDevices();

        Devices after = devicesDao.selectById(dev.getId());
        assertEquals(0, after.getStatus(), "超时未上报应置离线");
        List<DataQuality> rows = issuesOf(dev.getId(), "DEVICE_OFFLINE");
        assertFalse(rows.isEmpty());
        assertEquals("CRITICAL", rows.get(0).getSeverity());
    }

    @Test
    void qualityStatusReflectsIssues() {
        Devices dev = insertDevice("E2E-QUALITY-STAT", "AIR");
        // 初始应为 GOOD（该设备无任何问题记录）
        assertEquals("GOOD", dataQualityService.resolveStatus(dev.getId()));
        report(dev.getDeviceCode(), "PM25", new BigDecimal("9999"));
        assertEquals("BAD", dataQualityService.resolveStatus(dev.getId()), "存在 CRITICAL 应判 BAD");
    }

    @Test
    void qualityEndpointsWork() throws Exception {
        Devices dev = insertDevice("E2E-QUALITY-EP", "AIR");
        report(dev.getDeviceCode(), "PM25", new BigDecimal("9999"));
        mockMvc.perform(get("/api/quality/issues").param("deviceId", String.valueOf(dev.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data[0].issueType").value("OUT_OF_RANGE"));
        mockMvc.perform(get("/api/quality/status").param("deviceId", String.valueOf(dev.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.status").value("BAD"));
    }
}
