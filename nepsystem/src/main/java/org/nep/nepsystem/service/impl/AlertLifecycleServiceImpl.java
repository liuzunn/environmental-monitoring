package org.nep.nepsystem.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.nep.nepsystem.bean.Alerts;
import org.nep.nepsystem.dao.AlertsDao;
import org.nep.nepsystem.exception.BizException;
import org.nep.nepsystem.service.AlertLifecycleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 告警生命周期实现。
 * 状态机：
 *   新建(WARN/ALARM) -> ACKNOWLEDGED -> PROCESSING -> RESOLVED
 *   任意未解决状态 --(指标恢复正常, 超过自动恢复保持窗口)--> NORMAL
 * status 字段兼容同步：RESOLVED/NORMAL => 1，其余 => 0（保证旧接口 unhandled/page?status 语义不变）。
 */
@Service
public class AlertLifecycleServiceImpl implements AlertLifecycleService {

    private static final String USER_DEFAULT = "admin";
    private static final String USER_SYSTEM = "SYSTEM";

    /** 可进行流转的未解决状态集合 */
    private static final Set<String> OPEN_STATES = new HashSet<>(
            Arrays.asList("WARN", "ALARM", "ACKNOWLEDGED", "PROCESSING"));

    @Autowired
    private AlertsDao alertsDao;

    /** 自动恢复保持窗口：告警产生后至少保持该时长才允许被自动恢复（默认 60s） */
    @Value("${alert.auto-resolve-hold-ms:60000}")
    private long autoResolveHoldMs;

    private Alerts getOrThrow(Long id) {
        Alerts alert = alertsDao.selectById(id);
        if (alert == null) {
            throw new BizException(400, "告警不存在: " + id);
        }
        return alert;
    }

    @Override
    public Alerts acknowledge(Long id, String user) {
        Alerts alert = getOrThrow(id);
        if ("RESOLVED".equals(alert.getState()) || "NORMAL".equals(alert.getState())) {
            throw new BizException(400, "告警已结束，不能确认: " + id);
        }
        Date now = new Date();
        alert.setState("ACKNOWLEDGED");
        alert.setAckTime(now);
        alert.setAckUser(user != null && !user.isEmpty() ? user : USER_DEFAULT);
        alert.setStatus(0);
        alertsDao.updateById(alert);
        return alert;
    }

    @Override
    public Alerts process(Long id, String user) {
        Alerts alert = getOrThrow(id);
        if ("RESOLVED".equals(alert.getState()) || "NORMAL".equals(alert.getState())) {
            throw new BizException(400, "告警已结束，不能处理: " + id);
        }
        Date now = new Date();
        alert.setState("PROCESSING");
        alert.setHandleUser(user != null && !user.isEmpty() ? user : USER_DEFAULT);
        alert.setHandleTime(now);
        alert.setStatus(0);
        alertsDao.updateById(alert);
        return alert;
    }

    @Override
    public Alerts resolve(Long id, String user) {
        Alerts alert = getOrThrow(id);
        Date now = new Date();
        boolean alreadyEnded = "RESOLVED".equals(alert.getState()) || "NORMAL".equals(alert.getState());
        if (!alreadyEnded) {
            alert.setState("RESOLVED");
            alert.setStatus(1);
            alert.setResolveTime(now);
            alert.setResolveUser(user != null && !user.isEmpty() ? user : USER_DEFAULT);
            if (alert.getCreateTime() != null) {
                long duration = (now.getTime() - alert.getCreateTime().getTime()) / 1000;
                alert.setDurationSeconds(duration < 0 ? 0 : duration);
            }
            alertsDao.updateById(alert);
        }
        return alert;
    }

    @Override
    public void autoResolveOnRecovery(Long deviceId, String sensorCode, Date now) {
        List<Alerts> open = alertsDao.selectList(new QueryWrapper<Alerts>()
                .eq("device_id", deviceId)
                .eq("sensor_code", sensorCode)
                .eq("status", 0)
                .in("state", OPEN_STATES)
                .orderByDesc("create_time")
                .last("limit 1"));
        if (open.isEmpty()) {
            return;
        }
        Alerts alert = open.get(0);
        // 保持窗口内不自动恢复，避免刚告警即恢复的抖动；
        // 注意：负时间差（系统时钟回拨）不阻塞恢复
        if (alert.getCreateTime() != null) {
            long ageMillis = now.getTime() - alert.getCreateTime().getTime();
            if (ageMillis >= 0 && ageMillis < autoResolveHoldMs) {
                return;
            }
        }
        alert.setState("NORMAL");
        alert.setStatus(1);
        alert.setResolveTime(now);
        alert.setResolveUser(USER_SYSTEM);
        if (alert.getCreateTime() != null) {
            long duration = (now.getTime() - alert.getCreateTime().getTime()) / 1000;
            alert.setDurationSeconds(duration < 0 ? 0 : duration);
        }
        alertsDao.updateById(alert);
    }
}
