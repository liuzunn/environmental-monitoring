package org.nep.nepsystem.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.nep.nepsystem.bean.DataQuality;
import org.nep.nepsystem.bean.Devices;
import org.nep.nepsystem.bean.Sensors;
import org.nep.nepsystem.bean.Thresholds;
import org.nep.nepsystem.dao.DataQualityDao;
import org.nep.nepsystem.dao.SensorsDao;
import org.nep.nepsystem.dao.ThresholdsDao;
import org.nep.nepsystem.service.AnomalyDetectionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 统计异常检测实现（业务层升级）。
 * 内存态：滚动窗口（每设备每指标最近 30 条）、连续超标计数——进程内维护，重启后重建（文档已注明）。
 */
@Service
public class AnomalyDetectionServiceImpl implements AnomalyDetectionService {

    private static final Logger log = LoggerFactory.getLogger(AnomalyDetectionServiceImpl.class);

    @Autowired
    private DataQualityDao dataQualityDao;

    @Autowired
    private ThresholdsDao thresholdsDao;

    @Autowired
    private SensorsDao sensorsDao;

    /** 滚动窗口大小 */
    @Value("${anomaly.window-size:30}")
    private int windowSize;

    /** Z-Score 判定阈值 */
    @Value("${anomaly.zscore-threshold:3.0}")
    private double zscoreThreshold;

    /** 计算 Z-Score 所需最小样本数 */
    @Value("${anomaly.zscore-min-samples:10}")
    private int zscoreMinSamples;

    /** 连续超标判定条数 */
    @Value("${anomaly.consecutive-min:3}")
    private int consecutiveMin;

    /** 每设备每指标滚动窗口: key = deviceId:sensorCode */
    private final Map<String, Deque<BigDecimal>> windowByKey = new ConcurrentHashMap<>();

    /** 连续超标计数: key = deviceId:sensorCode */
    private final Map<String, Integer> consecutiveByKey = new ConcurrentHashMap<>();

    private String key(Integer deviceId, String sensorCode) {
        return deviceId + ":" + sensorCode;
    }

    /** 设备级阈值优先，其次全局；返回 null 表示无配置 */
    private Thresholds effectiveThreshold(Integer deviceId, String sensorCode) {
        Thresholds thr = thresholdsDao.selectOne(new QueryWrapper<Thresholds>()
                .eq("device_id", deviceId).eq("sensor_code", sensorCode)
                .eq("enabled", 1).last("limit 1"));
        if (thr == null) {
            thr = thresholdsDao.selectOne(new QueryWrapper<Thresholds>()
                    .isNull("device_id").eq("sensor_code", sensorCode)
                    .eq("enabled", 1).last("limit 1"));
        }
        return thr;
    }

    private void upsertAnomaly(Integer deviceId, String sensorCode, String issueType, String severity,
                               String detail, BigDecimal value) {
        QueryWrapper<DataQuality> qw = new QueryWrapper<DataQuality>()
                .eq("device_id", deviceId)
                .eq("category", "ANOMALY")
                .eq("issue_type", issueType);
        if (sensorCode == null) {
            qw.isNull("sensor_code");
        } else {
            qw.eq("sensor_code", sensorCode);
        }
        DataQuality exist = dataQualityDao.selectOne(qw);
        Date now = new Date();
        if (exist == null) {
            DataQuality dq = new DataQuality();
            dq.setDeviceId(deviceId);
            dq.setSensorCode(sensorCode);
            dq.setCategory("ANOMALY");
            dq.setIssueType(issueType);
            dq.setSeverity(severity);
            dq.setDetail(detail);
            dq.setLatestValue(value);
            dq.setFirstSeen(now);
            dq.setLastSeen(now);
            dq.setOccurrenceCount(1);
            dataQualityDao.insert(dq);
        } else {
            DataQuality upd = new DataQuality();
            upd.setId(exist.getId());
            upd.setSeverity(severity);
            upd.setDetail(detail);
            upd.setLatestValue(value);
            upd.setLastSeen(now);
            upd.setOccurrenceCount(exist.getOccurrenceCount() == null ? 1 : exist.getOccurrenceCount() + 1);
            dataQualityDao.updateById(upd);
        }
    }

    @Override
    public void checkOnReport(Devices device, String sensorCode, BigDecimal value, Date reportTime) {
        String k = key(device.getId(), sensorCode);
        BigDecimal prev = null;
        Deque<BigDecimal> window = windowByKey.computeIfAbsent(k, x -> new ArrayDeque<>());
        synchronized (window) {
            prev = window.peekLast();
            window.addLast(value);
            while (window.size() > windowSize) {
                window.removeFirst();
            }
        }

        // ---------- 1) Z-Score（留一法：统计窗口不含当前值，避免异常值稀释均值/方差——掩蔽效应） ----------
        if (window.size() - 1 >= zscoreMinSamples) {
            double[] vals = new double[window.size() - 1];
            int i = 0;
            double sum = 0;
            for (BigDecimal b : window) {
                if (i >= vals.length) {
                    break; // 跳过最后一个（当前值）
                }
                double v = b.doubleValue();
                vals[i++] = v;
                sum += v;
            }
            double mean = sum / vals.length;
            double var = 0;
            for (double v : vals) {
                double d = v - mean;
                var += d * d;
            }
            double std = Math.sqrt(var / (vals.length - 1));
            if (std > 1e-9) {
                double z = Math.abs((value.doubleValue() - mean) / std);
                if (z > zscoreThreshold) {
                    upsertAnomaly(device.getId(), sensorCode, "ZSCORE", "CRITICAL",
                            sensorCode + " Z-Score=" + String.format("%.2f", z) + " 偏离均值 " + String.format("%.2f", mean), value);
                }
            }
        }

        // ---------- 2) 连续超标（阈值引擎配置驱动，与其并存） ----------
        Thresholds thr = effectiveThreshold(device.getId(), sensorCode);
        if (thr != null) {
            BigDecimal bound = thr.getAlarmMax() != null ? thr.getAlarmMax() : thr.getWarnMax();
            BigDecimal boundMin = thr.getAlarmMin() != null ? thr.getAlarmMin() : thr.getWarnMin();
            boolean exceed = false;
            boolean alarmLevel = false;
            if (bound != null && value.compareTo(bound) > 0) {
                exceed = true;
                alarmLevel = thr.getAlarmMax() != null;
            } else if (boundMin != null && value.compareTo(boundMin) < 0) {
                exceed = true;
                alarmLevel = thr.getAlarmMin() != null;
            }
            if (exceed) {
                int cnt = consecutiveByKey.getOrDefault(k, 0) + 1;
                consecutiveByKey.put(k, cnt);
                if (cnt >= consecutiveMin) {
                    upsertAnomaly(device.getId(), sensorCode, "CONSECUTIVE_EXCEED",
                            alarmLevel ? "CRITICAL" : "WARN",
                            sensorCode + " 连续 " + cnt + " 条超过阈值" + (alarmLevel ? "上限(报警)" : "上限(预警)") + "，当前值 " + value, value);
                }
            } else {
                consecutiveByKey.put(k, 0);
            }
        }

        // ---------- 3) 突变（3*std 或 20% 量程取大） ----------
        if (prev != null) {
            BigDecimal diff = value.subtract(prev).abs();
            Sensors sensor = sensorsDao.selectOne(new QueryWrapper<Sensors>().eq("sensor_code", sensorCode).last("limit 1"));
            double threshold = 0;
            if (sensor != null && sensor.getMinRange() != null && sensor.getMaxRange() != null) {
                threshold = Math.max(0.2 * (sensor.getMaxRange().doubleValue() - sensor.getMinRange().doubleValue()),
                        window.size() >= 2 ? 3 * stdOf(window) : 0);
            } else if (window.size() >= 2) {
                threshold = 3 * stdOf(window);
            }
            if (threshold > 0 && diff.doubleValue() > threshold) {
                upsertAnomaly(device.getId(), sensorCode, "SUDDEN_CHANGE", "CRITICAL",
                        sensorCode + " 统计突变: " + prev + " -> " + value + "（阈值 " + String.format("%.2f", threshold) + "）", value);
            }
        }
    }

    private double stdOf(Deque<BigDecimal> window) {
        double sum = 0;
        for (BigDecimal b : window) {
            sum += b.doubleValue();
        }
        double mean = sum / window.size();
        double var = 0;
        for (BigDecimal b : window) {
            double d = b.doubleValue() - mean;
            var += d * d;
        }
        return Math.sqrt(var / (window.size() - 1));
    }
}
