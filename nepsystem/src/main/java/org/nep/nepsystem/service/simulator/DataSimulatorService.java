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

import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 模拟数据采集器（业务层升级）：由场景引擎 SimulationProfile 驱动，
 * 模拟正常波动/日周期/早晚高峰/突发污染/连续超标/设备离线与恢复/异常数据。
 * 每台设备一个独立场景实例（按设备 ID 种子化随机数，可复现）。
 * 仍由 simulator.enabled=true 控制；仍调用 MonitorDataService.report()，
 * 因此 WebSocket 实时推送与阈值告警链路完全不变。
 */
@Service
@ConditionalOnProperty(prefix = "simulator", name = "enabled", havingValue = "true")
public class DataSimulatorService {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(DataSimulatorService.class);

    @Autowired
    private SimulatorConfig config;

    @Autowired
    private DevicesDao devicesDao;

    @Autowired
    private MonitorDataService monitorDataService;

    /** 全局轮次计数 */
    private final AtomicLong roundCounter = new AtomicLong(0);

    /** 设备场景实例: deviceId -> profile */
    private final Map<Long, SimulationProfile> profiles = new ConcurrentHashMap<>();

    /**
     * 定时任务：每 intervalMs 毫秒为一轮，遍历全部启用设备生成数据。
     * 离线窗口内的设备本轮不产生数据（由质量扫描标记离线，恢复后 report() 自动置回在线）。
     */
    @Scheduled(fixedDelayString = "${simulator.interval-ms:5000}")
    public void simulate() {
        if (!config.isEnabled()) {
            return;
        }
        long round = roundCounter.incrementAndGet();
        List<Devices> devices = devicesDao.selectList(new QueryWrapper<Devices>()
                .ne("status", 2)); // 排除停用设备
        for (Devices d : devices) {
            try {
                SimulationProfile profile = profiles.computeIfAbsent(d.getId().longValue(),
                        id -> new SimulationProfile(new Random(id * 7919L + 2026L),
                                config.isScenarioEnabled(),
                                config.getPollutionIntervalRounds(),
                                config.getPollutionDurationRounds(),
                                config.getOfflineIntervalRounds(),
                                config.getOfflineDurationRounds(),
                                config.getAbnormalProbability()));
                if (profile.isOffline(round)) {
                    log.debug("模拟器：{} 处于离线窗口，本轮跳过", d.getDeviceCode());
                    continue;
                }
                List<Map<String, Object>> items = profile.generate(d.getType(), round, new java.util.Date());
                if (items == null || items.isEmpty()) {
                    continue;
                }
                monitorDataService.report(d.getDeviceCode(), items, new java.util.Date());
                log.debug("模拟上报 {} -> {} 条", d.getDeviceCode(), items.size());
            } catch (Exception e) {
                log.warn("模拟上报失败 {}: {}", d.getDeviceCode(), e.getMessage());
            }
        }
    }
}
