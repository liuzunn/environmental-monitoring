package org.nep.nepsystem.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.nep.nepsystem.bean.DataQuality;
import org.nep.nepsystem.bean.Devices;
import org.nep.nepsystem.bean.Sensors;
import org.nep.nepsystem.config.SimulatorConfig;
import org.nep.nepsystem.dao.DataQualityDao;
import org.nep.nepsystem.dao.DevicesDao;
import org.nep.nepsystem.dao.SensorsDao;
import org.nep.nepsystem.service.DataQualityService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 数据质量检测实现（业务层升级）。
 * 检测项：
 *   NULL_VALUE        上报项缺字段（WARN，设备级）
 *   OUT_OF_RANGE      超出 sensors 量程（CRITICAL）
 *   SUDDEN_CHANGE     与上一条相比突变超过量程的 50%（CRITICAL）
 *   CONSTANT_VALUE    连续 5 条完全相同（WARN）
 *   INTERVAL_ABNORMAL 上报间隔超过 2x 配置间隔（WARN，周期扫描）
 *   DEVICE_OFFLINE    超过 3x 配置间隔未上报（CRITICAL，置离线，周期扫描）
 * 内存态说明：上一值/连续相同计数为进程内状态，重启后重建（文档已注明）。
 */
@Service
public class DataQualityServiceImpl implements DataQualityService {

    private static final Logger log = LoggerFactory.getLogger(DataQualityServiceImpl.class);

    public static final String CATEGORY_QUALITY = "QUALITY";
    public static final String CATEGORY_ANOMALY = "ANOMALY";

    @Autowired
    private DataQualityDao dataQualityDao;

    @Autowired
    private DevicesDao devicesDao;

    @Autowired
    private SensorsDao sensorsDao;

    @Autowired
    private SimulatorConfig simulatorConfig;

    /** 周期扫描总开关（默认开启；测试可关闭） */
    @Value("${quality.scan.enabled:true}")
    private boolean scanEnabled;

    /** 离线判定阈值(ms)，<=0 时取 3 x simulator.interval-ms */
    @Value("${quality.offline-threshold-ms:-1}")
    private long offlineThresholdMs;

    /** 连续相同值达到该次数判定为"长时间不变化" */
    @Value("${quality.constant-min-repeat:5}")
    private int constantMinRepeat;

    /** 上一值缓存: key = deviceId:sensorCode */
    private final Map<String, BigDecimal> lastValueByKey = new ConcurrentHashMap<>();

    /** 连续相同值计数: key = deviceId:sensorCode */
    private final Map<String, Integer> constantCountByKey = new ConcurrentHashMap<>();

    /** 指标字典缓存（60s 过期刷新） */
    private volatile Map<String, Sensors> sensorDictCache = Collections.emptyMap();
    private volatile long sensorDictLoadedAt = 0;

    private String key(Integer deviceId, String sensorCode) {
        return deviceId + ":" + sensorCode;
    }

    private Map<String, Sensors> sensorDict() {
        long now = System.currentTimeMillis();
        if (now - sensorDictLoadedAt > 60000) {
            Map<String, Sensors> m = new HashMap<>();
            for (Sensors s : sensorsDao.selectList(null)) {
                m.put(s.getSensorCode(), s);
            }
            sensorDictCache = m;
            sensorDictLoadedAt = now;
        }
        return sensorDictCache;
    }

    private void upsert(Integer deviceId, String sensorCode, String category, String issueType,
                        String severity, String detail, BigDecimal value) {
        QueryWrapper<DataQuality> qw = new QueryWrapper<DataQuality>()
                .eq("device_id", deviceId)
                .eq("category", category)
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
            dq.setCategory(category);
            dq.setIssueType(issueType);
            dq.setSeverity(severity);
            dq.setDetail(detail);
            dq.setLatestValue(value);
            dq.setFirstSeen(now);
            dq.setLastSeen(now);
            dq.setOccurrenceCount(1);
            dataQualityDao.insert(dq);
        } else {
            // 严重度取最高：CRITICAL > WARN > INFO
            String merged = mergeSeverity(exist.getSeverity(), severity);
            DataQuality upd = new DataQuality();
            upd.setId(exist.getId());
            upd.setSeverity(merged);
            upd.setDetail(detail);
            upd.setLatestValue(value);
            upd.setLastSeen(now);
            upd.setOccurrenceCount(exist.getOccurrenceCount() == null ? 1 : exist.getOccurrenceCount() + 1);
            dataQualityDao.updateById(upd);
        }
    }

    private String mergeSeverity(String a, String b) {
        int ra = rank(a);
        int rb = rank(b);
        return ra >= rb ? a : b;
    }

    private int rank(String s) {
        if ("CRITICAL".equals(s)) return 3;
        if ("WARN".equals(s)) return 2;
        return 1;
    }

    @Override
    public void checkOnReport(Devices device, String sensorCode, BigDecimal value, Date reportTime) {
        String k = key(device.getId(), sensorCode);
        BigDecimal prev = lastValueByKey.get(k);
        lastValueByKey.put(k, value);

        // 1) 超范围
        Sensors sensor = sensorDict().get(sensorCode);
        if (sensor != null && sensor.getMinRange() != null && sensor.getMaxRange() != null) {
            boolean low = value.compareTo(sensor.getMinRange()) < 0;
            boolean high = value.compareTo(sensor.getMaxRange()) > 0;
            if (low || high) {
                upsert(device.getId(), sensorCode, CATEGORY_QUALITY, "OUT_OF_RANGE", "CRITICAL",
                        sensorCode + " 超出量程 [" + sensor.getMinRange() + "," + sensor.getMaxRange() + "]，当前值 " + value, value);
            }
        }

        // 2) 突变（相对上一值超过量程跨度 50%）
        if (prev != null && sensor != null && sensor.getMinRange() != null && sensor.getMaxRange() != null) {
            BigDecimal span = sensor.getMaxRange().subtract(sensor.getMinRange());
            BigDecimal threshold = span.multiply(new BigDecimal("0.5")).abs();
            BigDecimal diff = value.subtract(prev).abs();
            if (threshold.compareTo(BigDecimal.ZERO) > 0 && diff.compareTo(threshold) > 0) {
                upsert(device.getId(), sensorCode, CATEGORY_QUALITY, "SUDDEN_CHANGE", "CRITICAL",
                        sensorCode + " 突变: " + prev + " -> " + value, value);
            }
        }

        // 3) 长时间不变化（连续相同值）
        Integer cnt = constantCountByKey.getOrDefault(k, 0);
        if (prev != null && prev.compareTo(value) == 0) {
            cnt = cnt + 1;
            if (cnt == constantMinRepeat) {
                upsert(device.getId(), sensorCode, CATEGORY_QUALITY, "CONSTANT_VALUE", "WARN",
                        sensorCode + " 连续 " + constantMinRepeat + " 条数值相同: " + value, value);
            }
        } else {
            cnt = 0;
        }
        constantCountByKey.put(k, cnt);
    }

    @Override
    public void recordNullValue(Devices device, Map<String, Object> item) {
        Object code = item != null ? item.get("sensorCode") : null;
        upsert(device.getId(), code != null ? code.toString() : null, CATEGORY_QUALITY, "NULL_VALUE", "WARN",
                "上报项缺少" + (code == null ? "指标编码" : "数值") + "，已跳过", null);
    }

    private long effectiveOfflineThresholdMs() {
        if (offlineThresholdMs > 0) {
            return offlineThresholdMs;
        }
        return Math.max(15000L, 3L * simulatorConfig.getIntervalMs());
    }

    @Override
    @Scheduled(fixedDelayString = "${quality.scan.interval-ms:60000}", initialDelayString = "${quality.scan.initial-delay-ms:30000}")
    public void scanDevices() {
        if (!scanEnabled) {
            return;
        }
        long offlineMs = effectiveOfflineThresholdMs();
        long intervalAnomalyMs = Math.max(offlineMs / 2, 2L * simulatorConfig.getIntervalMs());
        Date now = new Date();
        List<Devices> devices = devicesDao.selectList(new QueryWrapper<Devices>().ne("status", 2));
        for (Devices d : devices) {
            if (d.getLastReportTime() == null) {
                continue; // 从未上报过，不判定离线
            }
            long gap = now.getTime() - d.getLastReportTime().getTime();
            if (gap > offlineMs) {
                if (d.getStatus() != null && d.getStatus() == 1) {
                    Devices upd = new Devices();
                    upd.setId(d.getId());
                    upd.setStatus(0);
                    devicesDao.updateById(upd);
                    log.warn("设备离线检测: {} ({}) 超过 {}ms 未上报，置为离线", d.getDeviceCode(), d.getId(), gap);
                }
                upsert(d.getId(), null, CATEGORY_QUALITY, "DEVICE_OFFLINE", "CRITICAL",
                        "设备 " + d.getDeviceCode() + " 超过 " + (gap / 1000) + " 秒未上报", null);
            } else if (gap > intervalAnomalyMs) {
                upsert(d.getId(), null, CATEGORY_QUALITY, "INTERVAL_ABNORMAL", "WARN",
                        "设备 " + d.getDeviceCode() + " 上报间隔异常: " + (gap / 1000) + " 秒", null);
            }
        }
    }

    @Override
    public List<DataQuality> queryIssues(Integer deviceId, String category, String issueType, int limit) {
        QueryWrapper<DataQuality> qw = new QueryWrapper<>();
        if (deviceId != null) qw.eq("device_id", deviceId);
        if (category != null && !category.isEmpty()) qw.eq("category", category);
        if (issueType != null && !issueType.isEmpty()) qw.eq("issue_type", issueType);
        qw.orderByDesc("last_seen").last("limit " + Math.min(Math.max(limit, 1), 200));
        return dataQualityDao.selectList(qw);
    }

    @Override
    public String resolveStatus(Integer deviceId) {
        List<DataQuality> issues = queryIssues(deviceId, null, null, 200);
        boolean critical = false;
        boolean warn = false;
        for (DataQuality dq : issues) {
            if ("CRITICAL".equals(dq.getSeverity())) critical = true;
            else if ("WARN".equals(dq.getSeverity())) warn = true;
        }
        if (critical) return "BAD";
        if (warn) return "WARNING";
        return "GOOD";
    }
}
