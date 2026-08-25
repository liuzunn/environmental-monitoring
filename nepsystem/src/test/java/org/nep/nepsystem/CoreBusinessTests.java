package org.nep.nepsystem;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.junit.jupiter.api.Test;
import org.nep.nepsystem.bean.Alerts;
import org.nep.nepsystem.bean.Thresholds;
import org.nep.nepsystem.dao.AlertsDao;
import org.nep.nepsystem.dao.ThresholdsDao;
import org.nep.nepsystem.exception.BizException;
import org.nep.nepsystem.service.MonitorDataService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 核心业务单元测试：
 * 1. 阈值告警触发（WARN / ALARM 边界）
 * 2. 数据上报事务回滚（设备不存在时不留脏数据）
 * 注意：使用 @Transactional 保证测试数据自动回滚，不影响正式数据
 */
@SpringBootTest(properties = {"simulator.enabled=false"})
@Transactional
class CoreBusinessTests {

    @Autowired
    private MonitorDataService monitorDataService;

    @Autowired
    private ThresholdsDao thresholdsDao;

    @Autowired
    private AlertsDao alertsDao;

    /** 测试1：阈值判断——值超过报警上限应产生 ALARM，超过预警上限应产生 WARN */
    @Test
    void thresholdAlertTrigger() {
        // 给设备1的 CO2 配置全局阈值：预警 900 / 报警 1000
        Thresholds thr = new Thresholds();
        thr.setDeviceId(null);          // 全局
        thr.setSensorCode("CO2");
        thr.setWarnMax(new BigDecimal("900"));
        thr.setAlarmMax(new BigDecimal("1000"));
        thr.setEnabled(1);
        thresholdsDao.insert(thr);

        // 上报 950 → 应产生 WARN
        monitorDataService.report("DEV-AIR-001",
                Collections.singletonList(mapOf("CO2", "950")), new Date());
        // 上报 1100 → 应产生 ALARM
        monitorDataService.report("DEV-AIR-001",
                Collections.singletonList(mapOf("CO2", "1100")), new Date());

        List<Alerts> list = alertsDao.selectList(new QueryWrapper<Alerts>()
                .eq("sensor_code", "CO2"));
        assertFalse(list.isEmpty(), "应产生告警");
        // 950 应触发 WARN，1100 应触发 ALARM（集合断言，避免同秒排序不稳定）
        boolean hasWarn = list.stream().anyMatch(a -> "WARN".equals(a.getLevel())
                && a.getAlertValue().compareTo(new BigDecimal("950")) == 0);
        boolean hasAlarm = list.stream().anyMatch(a -> "ALARM".equals(a.getLevel())
                && a.getAlertValue().compareTo(new BigDecimal("1100")) == 0);
        assertTrue(hasWarn, "950 应触发 WARN");
        assertTrue(hasAlarm, "1100 应触发 ALARM");
    }

    /** 测试2：事务回滚——设备不存在时上报应抛异常且不留任何数据 */
    @Test
    void reportRollbackOnUnknownDevice() {
        long before = alertsDao.selectCount(null);
        // 设备编号不存在
        assertThrows(BizException.class, () ->
                monitorDataService.report("DEV-NOT-EXIST",
                        Collections.singletonList(mapOf("TEMP", "25")), new Date()));
        // 事务回滚：告警数不应变化（本轮未产生告警，数据也不应写入）
        long after = alertsDao.selectCount(null);
        assertEquals(before, after, "异常时不应写入任何数据");
    }

    private Map<String, Object> mapOf(String code, String value) {
        Map<String, Object> m = new HashMap<>();
        m.put("sensorCode", code);
        m.put("value", value);
        return m;
    }
}