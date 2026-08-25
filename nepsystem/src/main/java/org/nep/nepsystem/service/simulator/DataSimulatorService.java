package org.nep.nepsystem.service.simulator;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.nep.nepsystem.bean.Devices;
import org.nep.nepsystem.config.SimulatorConfig;
import org.nep.nepsystem.dao.DevicesDao;
import org.nep.nepsystem.service.MonitorDataService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * 模拟数据采集器：定时为每台启用设备生成指标数据并调用数据上报（触发阈值告警）。
 * 由 simulator.enabled=true 控制（默认 false，接真实硬件时关闭）。
 * 使用手写 Logger（不依赖 Lombok）。
 */
@Service
@ConditionalOnProperty(prefix = "simulator", name = "enabled", havingValue = "true")
public class DataSimulatorService {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(DataSimulatorService.class);

    private final Random random = new Random();

    @Autowired
    private SimulatorConfig config;

    @Autowired
    private DevicesDao devicesDao;

    @Autowired
    private MonitorDataService monitorDataService;

    /**
     * 定时任务：每 intervalMs 毫秒为一轮，遍历全部设备生成数据
     */
    @Scheduled(fixedDelayString = "${simulator.interval-ms:5000}")
    public void simulate() {
        if (!config.isEnabled()) {
            return;
        }
        List<Devices> devices = devicesDao.selectList(new QueryWrapper<Devices>()
                .ne("status", 2)); // 排除停用设备
        for (Devices d : devices) {
            try {
                List<Map<String, Object>> items = generateItems(d.getType());
                monitorDataService.report(d.getDeviceCode(), items, new java.util.Date());
                log.debug("模拟上报 {} -> {} 条", d.getDeviceCode(), items.size());
            } catch (Exception e) {
                log.warn("模拟上报失败 {}: {}", d.getDeviceCode(), e.getMessage());
            }
        }
    }

    /** 按设备类型生成指标数据（与 sensors 字典约定一致） */
    private List<Map<String, Object>> generateItems(String type) {
        List<Map<String, Object>> items = new ArrayList<>();
        if ("AIR".equals(type)) {
            items.add(item("TEMP", round(18 + random.nextDouble() * 17)));          // 18-35 ℃
            items.add(item("HUMI", round(30 + random.nextDouble() * 50)));          // 30-80 %
            double pm25 = random.nextDouble() * 150;
            if (random.nextDouble() < 0.05) {
                pm25 = 150 + random.nextDouble() * 150;                             // 5% 概率峰值 150-300
            }
            items.add(item("PM25", round(pm25)));
            items.add(item("CO2", round(350 + random.nextDouble() * 850)));         // 350-1200 ppm
        } else if ("WATER".equals(type)) {
            items.add(item("PH", round(6.5 + random.nextDouble() * 2.5)));          // 6.5-9.0
            items.add(item("TURBIDITY", round(random.nextDouble() * 20)));          // 0-20 NTU
            items.add(item("DO", round(4 + random.nextDouble() * 8)));              // 4-12 mg/L
            items.add(item("TEMP", round(15 + random.nextDouble() * 15)));          // 15-30 ℃
        } else if ("NOISE".equals(type)) {
            items.add(item("NOISE", round(30 + random.nextDouble() * 65)));         // 30-95 dB
        }
        return items;
    }

    private Map<String, Object> item(String sensorCode, double value) {
        Map<String, Object> m = new HashMap<>();
        m.put("sensorCode", sensorCode);
        m.put("value", value);
        return m;
    }

    private double round(double v) {
        return BigDecimal.valueOf(v).setScale(2, RoundingMode.HALF_UP).doubleValue();
    }
}