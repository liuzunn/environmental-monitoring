package org.nep.nepsystem.service;

import org.nep.nepsystem.bean.Devices;

import java.math.BigDecimal;
import java.util.Date;

/**
 * 统计异常检测服务（业务层升级，第一阶段：纯统计方法，不依赖外部 AI）：
 *   ZSCORE              滚动窗口 Z-Score（|z| > 阈值，默认 3）
 *   CONSECUTIVE_EXCEED  连续 N 条超过阈值配置（默认 3 条）
 *   SUDDEN_CHANGE       与上一值相比超过 max(3*std, 20%量程)
 * 与原有阈值告警引擎并存（阈值引擎负责 WARN/ALARM 告警，本服务负责统计异常记录）。
 */
public interface AnomalyDetectionService {

    /** 上报时逐指标统计异常检测 */
    void checkOnReport(Devices device, String sensorCode, BigDecimal value, Date reportTime);
}
