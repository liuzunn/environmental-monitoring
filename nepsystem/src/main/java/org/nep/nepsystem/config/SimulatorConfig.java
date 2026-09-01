package org.nep.nepsystem.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 模拟数据源配置：simulator.enabled=true 时启动定时上报
 * 配置示例（application.properties）：
 *   simulator.enabled=true
 *   simulator.interval-ms=5000
 */
@Component
@ConfigurationProperties(prefix = "simulator")
public class SimulatorConfig {
    /** 是否启用模拟器 */
    private boolean enabled = false;
    /** 上报间隔毫秒 */
    private long intervalMs = 5000;
    /** 场景引擎开关（业务层升级）：true=启用日周期/高峰/污染/离线/异常数据场景 */
    private boolean scenarioEnabled = true;
    /** 突发污染平均间隔（轮） */
    private long pollutionIntervalRounds = 40;
    /** 突发污染持续时间（轮） */
    private long pollutionDurationRounds = 8;
    /** 设备离线平均间隔（轮） */
    private long offlineIntervalRounds = 80;
    /** 设备离线持续时间（轮） */
    private long offlineDurationRounds = 15;
    /** 异常数据注入概率（每指标每轮） */
    private double abnormalProbability = 0.02;


    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }


    public boolean isScenarioEnabled() { return scenarioEnabled; }
    public void setScenarioEnabled(boolean scenarioEnabled) { this.scenarioEnabled = scenarioEnabled; }
    public long getPollutionIntervalRounds() { return pollutionIntervalRounds; }
    public void setPollutionIntervalRounds(long pollutionIntervalRounds) { this.pollutionIntervalRounds = pollutionIntervalRounds; }
    public long getPollutionDurationRounds() { return pollutionDurationRounds; }
    public void setPollutionDurationRounds(long pollutionDurationRounds) { this.pollutionDurationRounds = pollutionDurationRounds; }
    public long getOfflineIntervalRounds() { return offlineIntervalRounds; }
    public void setOfflineIntervalRounds(long offlineIntervalRounds) { this.offlineIntervalRounds = offlineIntervalRounds; }
    public long getOfflineDurationRounds() { return offlineDurationRounds; }
    public void setOfflineDurationRounds(long offlineDurationRounds) { this.offlineDurationRounds = offlineDurationRounds; }
    public double getAbnormalProbability() { return abnormalProbability; }
    public void setAbnormalProbability(double abnormalProbability) { this.abnormalProbability = abnormalProbability; }

    public long getIntervalMs() {
        return intervalMs;
    }

    public void setIntervalMs(long intervalMs) {
        this.intervalMs = intervalMs;
    }
}
