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

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public long getIntervalMs() {
        return intervalMs;
    }

    public void setIntervalMs(long intervalMs) {
        this.intervalMs = intervalMs;
    }
}
