package org.nep.nepsystem.service;

import org.nep.nepsystem.bean.DataQuality;
import org.nep.nepsystem.bean.Devices;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * 数据质量检测服务（业务层升级）：
 * 检测 NULL / 超范围 / 突变 / 长时间不变化 / 时间间隔异常 / 设备离线，
 * 结果写入 data_quality 表（快照式），并周期性扫描设备离线与上报间隔。
 * 与阈值告警、统计异常检测（AnomalyDetectionService）并存。
 */
public interface DataQualityService {

    /** 上报时逐指标质量检测（NULL 由 recordNullValue 处理，其余在此） */
    void checkOnReport(Devices device, String sensorCode, BigDecimal value, Date reportTime);

    /** 记录 NULL 值问题（上报项缺 sensorCode 或 value） */
    void recordNullValue(Devices device, Map<String, Object> item);

    /** 周期扫描：设备离线（DEVICE_OFFLINE）与上报间隔异常（INTERVAL_ABNORMAL） */
    void scanDevices();

    /** 查询质量/异常记录（deviceId/category/issueType 可空过滤，按 last_seen 倒序） */
    List<DataQuality> queryIssues(Integer deviceId, String category, String issueType, int limit);

    /** 设备质量状态：GOOD/WARNING/BAD（存在 CRITICAL 为 BAD，存在 WARN 为 WARNING） */
    String resolveStatus(Integer deviceId);
}
