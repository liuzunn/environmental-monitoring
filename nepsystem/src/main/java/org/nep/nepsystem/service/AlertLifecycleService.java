package org.nep.nepsystem.service;

import org.nep.nepsystem.bean.Alerts;

import java.util.Date;

/**
 * 告警生命周期服务（业务层升级）：
 * WARN/ALARM -> ACKNOWLEDGED -> PROCESSING -> RESOLVED
 * NORMAL：指标恢复正常时自动恢复（autoResolveOnRecovery）
 * 兼容性：state 与旧 status 字段保持同步（RESOLVED/NORMAL 对应 status=1，其余 status=0）。
 */
public interface AlertLifecycleService {

    /** 确认告警：WARN/ALARM -> ACKNOWLEDGED */
    Alerts acknowledge(Long id, String user);

    /** 开始处理：WARN/ALARM/ACKNOWLEDGED -> PROCESSING（同时写入处理人/处理时间） */
    Alerts process(Long id, String user);

    /** 解决告警：任意未解决状态 -> RESOLVED（status=1，写入解决时间/解决人/持续时间） */
    Alerts resolve(Long id, String user);

    /** 自动恢复：指标回到正常范围时，把该设备该指标最新未解决告警置为 NORMAL */
    void autoResolveOnRecovery(Long deviceId, String sensorCode, Date now);
}
