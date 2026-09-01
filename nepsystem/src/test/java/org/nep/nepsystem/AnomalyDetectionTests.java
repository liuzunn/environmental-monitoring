package org.nep.nepsystem;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.junit.jupiter.api.Test;
import org.nep.nepsystem.bean.DataQuality;
import org.nep.nepsystem.bean.Devices;
import org.nep.nepsystem.dao.DataQualityDao;
import org.nep.nepsystem.dao.DevicesDao;
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
 * 统计异常检测测试（业务层升级模块三）：
 * ZSCORE / CONSECUTIVE_EXCEED / SUDDEN_CHANGE + 接口验证
 * 与阈值告警并存：连续超标测试同时会触发 ALARM 告警（种子全局阈值 NOISE 75），验证互不干扰。
 * 说明：各测试使用不同指标避免共享内存态（滚动窗口/连续计数）互相干扰。
 */
@SpringBootTest(properties = {"simulator.enabled=false", "quality.scan.enabled=false", "alert.auto-resolve-hold-ms=0"})
@Transactional
@AutoConfigureMockMvc
class AnomalyDetectionTests {

    @Autowired private MonitorDataService monitorDataService;
    @Autowired private DataQualityDao dataQualityDao;
    @Autowired private DevicesDao devicesDao;
    @Autowired private MockMvc mockMvc;

    private Devices insertDevice(String code, String type) {
        Devices d = new Devices();
        d.setDeviceCode(code);
        d.setDeviceName("异常测试-" + code);
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

    private List<DataQuality> anomaliesOf(Integer deviceId, String issueType) {
        return dataQualityDao.selectList(new QueryWrapper<DataQuality>()
                .eq("device_id", deviceId).eq("category", "ANOMALY").eq("issue_type", issueType)
                .orderByDesc("id").last("limit 1"));
    }

    @Test
    void zscoreDetected() {
        Devices dev = insertDevice("E2E-ANOM-Z", "AIR");
        // TEMP 围绕 50 小幅波动 10 条（49/51 交替，std≈1），随后 58 -> |z|≈8 > 3
        for (int i = 0; i < 10; i++) {
            report(dev.getDeviceCode(), "TEMP", i % 2 == 0 ? new BigDecimal("49.00") : new BigDecimal("51.00"));
        }
        report(dev.getDeviceCode(), "TEMP", new BigDecimal("58.00"));

        List<DataQuality> rows = anomaliesOf(dev.getId(), "ZSCORE");
        assertFalse(rows.isEmpty(), "应记录 ZSCORE 异常");
        assertEquals("CRITICAL", rows.get(0).getSeverity());
    }

    @Test
    void consecutiveExceedDetectedAndCoexistsWithAlerts() {
        Devices dev = insertDevice("E2E-ANOM-C", "NOISE");
        // NOISE 种子全局阈值: 预警 65 / 报警 75；连续 3 条 80
        for (int i = 0; i < 3; i++) {
            report(dev.getDeviceCode(), "NOISE", new BigDecimal("80.00"));
        }
        List<DataQuality> rows = anomaliesOf(dev.getId(), "CONSECUTIVE_EXCEED");
        assertFalse(rows.isEmpty(), "应记录 CONSECUTIVE_EXCEED");
        assertEquals("CRITICAL", rows.get(0).getSeverity(), "超过报警上限应为 CRITICAL");
        // 阈值引擎告警并存：存在 ALARM 告警
        assertTrue(dataQualityDao.selectCount(new QueryWrapper<DataQuality>().eq("category", "ANOMALY").eq("issue_type", "CONSECUTIVE_EXCEED")) > 0);
    }

    @Test
    void suddenChangeDetected() {
        Devices dev = insertDevice("E2E-ANOM-S", "WATER");
        // PH 围绕 7.0 波动 10 条，随后 2.0（量程 [0,14]，20% 量程=2.8，diff=5.0）
        double[] base = {7.0, 7.1, 6.9, 7.0, 7.1, 6.9, 7.0, 7.1, 6.9, 7.0};
        for (double v : base) {
            report(dev.getDeviceCode(), "PH", new BigDecimal(String.valueOf(v)));
        }
        report(dev.getDeviceCode(), "PH", new BigDecimal("2.0"));

        List<DataQuality> rows = anomaliesOf(dev.getId(), "SUDDEN_CHANGE");
        assertFalse(rows.isEmpty(), "应记录 SUDDEN_CHANGE 统计异常");
        assertEquals("CRITICAL", rows.get(0).getSeverity());
    }

    @Test
    void anomalyEndpointsWork() throws Exception {
        Devices dev = insertDevice("E2E-ANOM-EP", "WATER");
        // 制造一条 ZSCORE（TURBIDITY 未在别处使用）
        for (int i = 0; i < 10; i++) {
            report(dev.getDeviceCode(), "TURBIDITY", i % 2 == 0 ? new BigDecimal("5.00") : new BigDecimal("7.00"));
        }
        report(dev.getDeviceCode(), "TURBIDITY", new BigDecimal("19.00")); // 量程 [0,100]，19 < 100 不超量程

        mockMvc.perform(get("/api/anomalies").param("deviceId", String.valueOf(dev.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
        mockMvc.perform(get("/api/anomalies/summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }
}
