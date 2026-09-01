package org.nep.nepsystem.service.simulator;

import java.util.*;

/**
 * 模拟场景引擎（业务层升级，替代简单 random）：
 *  - 正常波动：随机游走（平滑增量，非独立随机）
 *  - 日周期：正弦曲线（各指标峰值时段不同）
 *  - 早晚高峰：07-09 与 17-19 梯形增强（PM25/CO2/NOISE）
 *  - 突发污染：周期性污染事件（PM25/TURBIDITY 抬升、DO 下降），保持多轮以触发连续超标
 *  - 设备离线/恢复：周期性离线窗口（generate 返回 null），窗口结束自动恢复
 *  - 异常数据：小概率注入超量程/突变/缺值
 * 纯逻辑类（无 Spring 依赖），便于单元测试。量程/基值/步长与 sensors 字典保持一致。
 */
public class SimulationProfile {

    private static final Map<String, String[]> TYPE_SENSORS = new HashMap<>();
    private static final Map<String, double[]> SENSOR_RANGE = new HashMap<>();
    private static final Map<String, Double> SENSOR_BASE = new HashMap<>();
    private static final Map<String, Double> SENSOR_STEP = new HashMap<>();
    private static final Map<String, Double> SENSOR_AMPLITUDE = new HashMap<>();

    static {
        TYPE_SENSORS.put("AIR", new String[]{"TEMP", "HUMI", "PM25", "CO2"});
        TYPE_SENSORS.put("WATER", new String[]{"PH", "TURBIDITY", "DO", "TEMP"});
        TYPE_SENSORS.put("NOISE", new String[]{"NOISE"});

        SENSOR_RANGE.put("TEMP", new double[]{-10, 60});
        SENSOR_RANGE.put("HUMI", new double[]{0, 100});
        SENSOR_RANGE.put("PM25", new double[]{0, 500});
        SENSOR_RANGE.put("CO2", new double[]{300, 5000});
        SENSOR_RANGE.put("PH", new double[]{0, 14});
        SENSOR_RANGE.put("TURBIDITY", new double[]{0, 100});
        SENSOR_RANGE.put("DO", new double[]{0, 20});
        SENSOR_RANGE.put("NOISE", new double[]{20, 130});

        SENSOR_BASE.put("TEMP", 24.0);
        SENSOR_BASE.put("HUMI", 55.0);
        SENSOR_BASE.put("PM25", 30.0);
        SENSOR_BASE.put("CO2", 600.0);
        SENSOR_BASE.put("PH", 7.3);
        SENSOR_BASE.put("TURBIDITY", 3.5);
        SENSOR_BASE.put("DO", 7.5);
        SENSOR_BASE.put("NOISE", 45.0);

        SENSOR_STEP.put("TEMP", 0.3);
        SENSOR_STEP.put("HUMI", 0.8);
        SENSOR_STEP.put("PM25", 2.5);
        SENSOR_STEP.put("CO2", 25.0);
        SENSOR_STEP.put("PH", 0.05);
        SENSOR_STEP.put("TURBIDITY", 0.3);
        SENSOR_STEP.put("DO", 0.1);
        SENSOR_STEP.put("NOISE", 1.5);

        SENSOR_AMPLITUDE.put("TEMP", 3.0);
        SENSOR_AMPLITUDE.put("HUMI", 8.0);
        SENSOR_AMPLITUDE.put("PM25", 8.0);
        SENSOR_AMPLITUDE.put("CO2", 120.0);
        SENSOR_AMPLITUDE.put("PH", 0.1);
        SENSOR_AMPLITUDE.put("TURBIDITY", 0.5);
        SENSOR_AMPLITUDE.put("DO", 0.3);
        SENSOR_AMPLITUDE.put("NOISE", 8.0);
    }

    private final Random random;
    private final boolean scenarioEnabled;
    private final long pollutionIntervalRounds;
    private final long pollutionDurationRounds;
    private final long offlineIntervalRounds;
    private final long offlineDurationRounds;
    private final double abnormalProbability;

    /** 各指标当前值（随机游走状态） */
    private final Map<String, Double> current = new HashMap<>();

    private long nextPollutionRound = -1;
    private long pollutionUntilRound = -1;
    private String pollutionSensor;
    private double pollutionTarget;

    private long nextOfflineRound = -1;
    private long offlineUntilRound = -1;

    public SimulationProfile(Random random, boolean scenarioEnabled, long pollutionIntervalRounds,
                             long pollutionDurationRounds, long offlineIntervalRounds,
                             long offlineDurationRounds, double abnormalProbability) {
        this.random = random;
        this.scenarioEnabled = scenarioEnabled;
        this.pollutionIntervalRounds = Math.max(2, pollutionIntervalRounds);
        this.pollutionDurationRounds = Math.max(2, pollutionDurationRounds);
        this.offlineIntervalRounds = Math.max(3, offlineIntervalRounds);
        this.offlineDurationRounds = Math.max(1, offlineDurationRounds);
        this.abnormalProbability = abnormalProbability;
    }

    /** 是否处于离线窗口（离线时不产生数据） */
    public boolean isOffline(long round) {
        return round < offlineUntilRound;
    }

    /**
     * 生成一轮指标数据；设备处于离线窗口返回 null。
     * @param deviceType AIR/WATER/NOISE
     * @param round      全局轮次
     * @param now        当前时间（用于日周期/高峰）
     */
    public List<Map<String, Object>> generate(String deviceType, long round, Date now) {
        if (isOffline(round)) {
            return null;
        }
        if (scenarioEnabled) {
            scheduleAndFireEvents(round);
        }
        Calendar cal = Calendar.getInstance();
        cal.setTime(now);
        int hour = cal.get(Calendar.HOUR_OF_DAY);
        String[] sensors = TYPE_SENSORS.getOrDefault(deviceType, TYPE_SENSORS.get("AIR"));
        List<Map<String, Object>> items = new ArrayList<>();
        for (String code : sensors) {
            double normal = normalTarget(code, hour);
            double v = applyPollution(code, normal, round);
            double prev = current.getOrDefault(code, SENSOR_BASE.getOrDefault(code, 0.0));
            // 平滑随机游走：向目标靠拢 + 小步噪声
            double walk = prev + (v - prev) * 0.35 + (random.nextDouble() - 0.5) * SENSOR_STEP.getOrDefault(code, 0.5);
            double[] range = SENSOR_RANGE.getOrDefault(code, new double[]{0, 100});
            v = Math.min(range[1], Math.max(range[0], walk));
            current.put(code, v);

            // 异常数据注入（小概率）
            if (scenarioEnabled && random.nextDouble() < abnormalProbability) {
                int kind = random.nextInt(3);
                if (kind == 0) {
                    // 超量程
                    v = range[1] + 10 + random.nextDouble() * 200;
                } else if (kind == 1) {
                    // 突变（大幅度跳变）
                    v = Math.min(99999, Math.abs(v + (random.nextBoolean() ? 1 : -1) * (range[1] - range[0]) * 0.6));
                } else {
                    // 缺值（触发 NULL 检测）
                    Map<String, Object> missing = new HashMap<>();
                    missing.put("sensorCode", code);
                    items.add(missing);
                    continue;
                }
            }
            Map<String, Object> item = new HashMap<>();
            item.put("sensorCode", code);
            item.put("value", Math.round(v * 100.0) / 100.0);
            items.add(item);
        }
        return items;
    }

    /** 事件调度：突发污染 与 离线窗口（按平均间隔随机化） */
    private void scheduleAndFireEvents(long round) {
        if (nextPollutionRound < 0) {
            nextPollutionRound = round + 1 + random.nextInt((int) Math.max(1, pollutionIntervalRounds));
        }
        if (nextOfflineRound < 0) {
            nextOfflineRound = round + 1 + random.nextInt((int) Math.max(1, offlineIntervalRounds));
        }
        if (round >= nextPollutionRound && round >= pollutionUntilRound) {
            startPollution(round);
            nextPollutionRound = round + pollutionDurationRounds + random.nextInt((int) Math.max(1, pollutionIntervalRounds));
        }
        if (round >= nextOfflineRound && round >= offlineUntilRound) {
            offlineUntilRound = round + offlineDurationRounds;
            nextOfflineRound = offlineUntilRound + random.nextInt((int) Math.max(1, offlineIntervalRounds));
        }
    }

    private void startPollution(long round) {
        String[] candidates = {"PM25", "TURBIDITY", "NOISE", "DO", "CO2"};
        pollutionSensor = candidates[random.nextInt(candidates.length)];
        double[] range = SENSOR_RANGE.getOrDefault(pollutionSensor, new double[]{0, 100});
        double span = range[1] - range[0];
        // 目标取量程上部 60%-80%，保证超过常见告警阈值并保持多轮（连续超标）
        pollutionTarget = range[0] + span * (0.6 + random.nextDouble() * 0.2);
        pollutionUntilRound = round + pollutionDurationRounds;
    }

    /** 污染事件期间按 抬升->维持->衰减 轨迹覆盖目标值；DO 特殊：污染时下降（越低越差） */
    private double applyPollution(String code, double normal, long round) {
        if (round >= pollutionUntilRound || !pollutionSensor.equals(code)) {
            return normal;
        }
        long total = pollutionDurationRounds;
        long ramp = Math.max(1, total / 4);
        long decay = Math.max(1, total / 4);
        long hold = total - ramp - decay;
        long phase = round - (pollutionUntilRound - total);
        double lowTarget = SENSOR_BASE.getOrDefault(code, 0.0);
        if ("DO".equals(code)) {
            // 溶解氧污染：向低值滑落
            if (phase <= ramp) return Math.max(0, lowTarget - (lowTarget - 2.0) * phase / ramp);
            if (phase <= ramp + hold) return 2.0;
            return Math.min(lowTarget, 2.0 + (lowTarget - 2.0) * (phase - ramp - hold) / decay);
        }
        if (phase <= ramp) return normal + (pollutionTarget - normal) * phase / ramp;
        if (phase <= ramp + hold) return pollutionTarget;
        return pollutionTarget - (pollutionTarget - normal) * (phase - ramp - hold) / decay;
    }

    /** 正常目标值 = 基值 + 日周期正弦 + 早晚高峰增强（仅空气/噪声类） */
    private double normalTarget(String code, int hour) {
        double base = SENSOR_BASE.getOrDefault(code, 0.0);
        double amp = SENSOR_AMPLITUDE.getOrDefault(code, 0.0);
        // 日周期：峰值时段因指标而异（TEMP 15 点最高、PM25/CO2/NOISE 早晚双峰由 rush 补充）
        int peakHour = "TEMP".equals(code) ? 15 : ("HUMI".equals(code) ? 5 : 12);
        double cycle = amp * Math.sin((hour - peakHour) / 24.0 * 2 * Math.PI);
        double rush = 1.0;
        if ("PM25".equals(code) || "CO2".equals(code) || "NOISE".equals(code)) {
            double boost = 0;
            boost = Math.max(boost, rushFactor(hour, 8));   // 早高峰 07-09
            boost = Math.max(boost, rushFactor(hour, 18));  // 晚高峰 17-19
            rush = 1 + boost * 0.5;
        }
        double target = (base + cycle) * rush;
        double[] range = SENSOR_RANGE.getOrDefault(code, new double[]{0, 100});
        return Math.min(range[1], Math.max(range[0], target));
    }

    /** 梯形高峰因子：center±2 小时线性增强，0-1 */
    private double rushFactor(int hour, int center) {
        int diff = Math.abs(hour - center);
        if (diff >= 2) return 0;
        return 1 - diff / 2.0;
    }
}
