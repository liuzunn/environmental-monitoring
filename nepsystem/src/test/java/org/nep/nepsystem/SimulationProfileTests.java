package org.nep.nepsystem;

import org.junit.jupiter.api.Test;
import org.nep.nepsystem.service.simulator.SimulationProfile;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 模拟场景引擎单元测试（业务层升级模块五，纯逻辑无 Spring/DB）：
 * 1. 正常波动始终在量程内
 * 2. 日周期 + 早晚高峰（8 点 PM25 均值 > 3 点）
 * 3. 突发污染产生连续超标（≥3 轮高于量程 60%）
 * 4. 离线窗口跳过且自动恢复
 * 5. 异常数据注入（超量程/缺值）
 */
class SimulationProfileTests {

    private SimulationProfile profile(long seed, boolean scenario, long pollInt, long pollDur,
                                      long offInt, long offDur, double abnormal) {
        return new SimulationProfile(new Random(seed), scenario, pollInt, pollDur, offInt, offDur, abnormal);
    }

    private Date atHour(int hour) {
        Calendar c = Calendar.getInstance();
        c.set(2026, Calendar.SEPTEMBER, 1, hour, 0, 0);
        return c.getTime();
    }

    @Test
    void valuesAlwaysWithinRange() {
        SimulationProfile p = profile(42, true, 40, 8, 80, 15, 0);
        Map<String, double[]> ranges = new HashMap<>();
        ranges.put("TEMP", new double[]{-10, 60});
        ranges.put("HUMI", new double[]{0, 100});
        ranges.put("PM25", new double[]{0, 500});
        ranges.put("CO2", new double[]{300, 5000});
        for (long round = 1; round <= 300; round++) {
            List<Map<String, Object>> items = p.generate("AIR", round, atHour(12));
            if (items == null) continue;
            for (Map<String, Object> item : items) {
                double v = ((Number) item.get("value")).doubleValue();
                double[] r = ranges.get(item.get("sensorCode"));
                assertTrue(v >= r[0] && v <= r[1], "值 " + v + " 超出量程 " + Arrays.toString(r));
            }
        }
    }

    @Test
    void rushHourHigherThanNight() {
        // 关场景只留日周期+高峰；两个独立 profile（相同种子）分别喂 8 点与 3 点，避免游走状态互相污染
        SimulationProfile p8 = profile(7, false, 40, 8, 80, 15, 0);
        SimulationProfile p3 = profile(7, false, 40, 8, 80, 15, 0);
        double sum8 = 0, sum3 = 0;
        int n = 60;
        for (int i = 0; i < n; i++) {
            List<Map<String, Object>> items8 = p8.generate("AIR", i + 1, atHour(8));
            List<Map<String, Object>> items3 = p3.generate("AIR", i + 1, atHour(3));
            for (Map<String, Object> it : items8) {
                if ("PM25".equals(it.get("sensorCode"))) sum8 += ((Number) it.get("value")).doubleValue();
            }
            for (Map<String, Object> it : items3) {
                if ("PM25".equals(it.get("sensorCode"))) sum3 += ((Number) it.get("value")).doubleValue();
            }
        }
        double mean8 = sum8 / n, mean3 = sum3 / n;
        assertTrue(mean8 > mean3 + 5, "早高峰 PM25 均值应显著高于凌晨: 8h=" + mean8 + " 3h=" + mean3);
    }

    @Test
    void pollutionCausesConsecutiveExceedance() {
        // 短间隔小规模污染，观察连续超标
        SimulationProfile p = profile(11, true, 2, 8, 9999, 15, 0);
        Map<String, Integer> consecutiveHigh = new HashMap<>();
        int maxConsecutive = 0;
        for (long round = 1; round <= 40; round++) {
            List<Map<String, Object>> items = p.generate("AIR", round, atHour(12));
            if (items == null) continue;
            for (Map<String, Object> it : items) {
                String code = (String) it.get("sensorCode");
                double v = ((Number) it.get("value")).doubleValue();
                double[] range = code.equals("PM25") ? new double[]{0, 500} : (code.equals("CO2") ? new double[]{300, 5000} : new double[]{0, 100});
                boolean high = v > range[0] + (range[1] - range[0]) * 0.6;
                int cnt = consecutiveHigh.getOrDefault(code, 0);
                cnt = high ? cnt + 1 : 0;
                consecutiveHigh.put(code, cnt);
                maxConsecutive = Math.max(maxConsecutive, cnt);
            }
        }
        assertTrue(maxConsecutive >= 3, "突发污染应产生连续超标（实际最大连续 " + maxConsecutive + " 轮）");
    }

    @Test
    void offlineWindowSkipsThenRecovers() {
        SimulationProfile p = profile(5, true, 9999, 8, 3, 5, 0);
        boolean sawOffline = false;
        boolean recovered = false;
        for (long round = 1; round <= 30; round++) {
            List<Map<String, Object>> items = p.generate("AIR", round, atHour(12));
            if (items == null) {
                sawOffline = true;
                // 离线后必然后续恢复
            } else if (sawOffline) {
                recovered = true;
            }
        }
        assertTrue(sawOffline, "应出现离线窗口");
        assertTrue(recovered, "离线窗口后应自动恢复上报");
    }

    @Test
    void abnormalInjectionProducesBadData() {
        SimulationProfile p = profile(3, true, 9999, 8, 9999, 15, 1.0);
        boolean sawOutOfRange = false;
        boolean sawMissing = false;
        for (long round = 1; round <= 20; round++) {
            List<Map<String, Object>> items = p.generate("AIR", round, atHour(12));
            if (items == null) continue;
            for (Map<String, Object> it : items) {
                if (!it.containsKey("value")) {
                    sawMissing = true;
                } else {
                    double v = ((Number) it.get("value")).doubleValue();
                    String code = (String) it.get("sensorCode");
                    double[] range = code.equals("PM25") ? new double[]{0, 500} : (code.equals("CO2") ? new double[]{300, 5000} : new double[]{0, 100});
                    if (v > range[1]) sawOutOfRange = true;
                }
            }
        }
        assertTrue(sawOutOfRange || sawMissing, "异常注入应产生超量程或缺值数据");
    }
}
