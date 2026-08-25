package org.nep.nepsystem.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.nep.nepsystem.bean.Alerts;
import org.nep.nepsystem.bean.Devices;
import org.nep.nepsystem.bean.MonitorData;
import org.nep.nepsystem.bean.Thresholds;
import org.nep.nepsystem.dao.AlertsDao;
import org.nep.nepsystem.dao.DevicesDao;
import org.nep.nepsystem.dao.MonitorDataDao;
import org.nep.nepsystem.dao.ThresholdsDao;
import org.nep.nepsystem.exception.BizException;
import org.nep.nepsystem.service.MonitorDataService;
import org.nep.nepsystem.ws.NotifyWebSocketHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Calendar;
import java.util.List;
import java.util.Map;

/**
 * 监测数据业务实现
 */
@Service
public class MonitorDataServiceImpl extends ServiceImpl<MonitorDataDao, MonitorData> implements MonitorDataService {

    @Override
    public boolean save(MonitorData entity) {
        return super.save(entity);
    }

    @Override
    public boolean update(MonitorData entity) {
        return super.updateById(entity);
    }

    @Override
    public boolean deleteById(java.io.Serializable id) {
        return super.removeById(id);
    }

    @Override
    public MonitorData getById(java.io.Serializable id) {
        return super.getById(id);
    }

    @Override
    public IPage<MonitorData> page(int page, int size) {
        return this.page(new Page<>(page, size), new QueryWrapper<>());
    }

    @Autowired
    private DevicesDao devicesDao;

    @Autowired
    private ThresholdsDao thresholdsDao;

    @Autowired
    private AlertsDao alertsDao;

    @Autowired
    private NotifyWebSocketHandler notifyWebSocketHandler;

    /**
     * 数据上报：写数据 + 更新设备状态 + 阈值告警，整体一个事务
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void report(String deviceCode, List<Map<String, Object>> items, java.util.Date reportTime) {
        if (deviceCode == null || items == null || items.isEmpty()) {
            throw new BizException(400, "设备编号与指标数据不能为空");
        }
        Devices device = devicesDao.selectOne(new QueryWrapper<Devices>()
                .eq("device_code", deviceCode).last("limit 1"));
        if (device == null) {
            throw new BizException("设备不存在: " + deviceCode);
        }
        java.util.Date now = reportTime != null ? reportTime : new java.util.Date();

        // 1) 写入监测数据
        for (Map<String, Object> item : items) {
            Object codeObj = item.get("sensorCode");
            Object valObj = item.get("value");
            if (codeObj == null || valObj == null) {
                continue;
            }
            MonitorData md = new MonitorData();
            md.setDeviceId(device.getId());
            md.setSensorCode(codeObj.toString());
            md.setValue(new BigDecimal(valObj.toString()));
            md.setReportTime(now);
            super.save(md);
            // WebSocket 实时广播
            notifyWebSocketHandler.broadcast("{\"type\":\"data\",\"deviceId\":" + device.getId()
                    + ",\"deviceCode\":\"" + deviceCode + "\",\"sensorCode\":\"" + codeObj + "\",\"value\":"
                    + valObj + ",\"reportTime\":\"" + new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(now) + "\"}");
        }

        // 2) 更新设备在线状态与最近上报时间
        Devices upd = new Devices();
        upd.setId(device.getId());
        upd.setStatus(1);
        upd.setLastReportTime(now);
        devicesDao.updateById(upd);

        // 3) 阈值校验并写入告警
        for (Map<String, Object> item : items) {
            Object codeObj = item.get("sensorCode");
            Object valObj = item.get("value");
            if (codeObj == null || valObj == null) {
                continue;
            }
            String code = codeObj.toString();
            BigDecimal val = new BigDecimal(valObj.toString());
            checkThreshold(device, code, val, now);
        }
    }

    /**
     * 阈值校验：设备级阈值优先，其次全局阈值；超过报警区间记 ALARM，超过预警区间记 WARN。
     * 同一设备同一指标同一级别 30 分钟内不重复告警。
     */
    private void checkThreshold(Devices device, String sensorCode, BigDecimal value, java.util.Date now) {
        // 设备级阈值
        Thresholds thr = thresholdsDao.selectOne(new QueryWrapper<Thresholds>()
                .eq("device_id", device.getId()).eq("sensor_code", sensorCode)
                .eq("enabled", 1).last("limit 1"));
        if (thr == null) {
            // 全局默认阈值
            thr = thresholdsDao.selectOne(new QueryWrapper<Thresholds>()
                    .isNull("device_id").eq("sensor_code", sensorCode)
                    .eq("enabled", 1).last("limit 1"));
        }
        if (thr == null) {
            return; // 无阈值配置，不告警
        }
        String level = null;
        String desc = null;
        // 报警优先判断
        if (thr.getAlarmMax() != null && value.compareTo(thr.getAlarmMax()) > 0) {
            level = "ALARM";
            desc = sensorCode + " 超过报警上限 " + thr.getAlarmMax() + "，当前值 " + value;
        } else if (thr.getAlarmMin() != null && value.compareTo(thr.getAlarmMin()) < 0) {
            level = "ALARM";
            desc = sensorCode + " 低于报警下限 " + thr.getAlarmMin() + "，当前值 " + value;
        } else if (thr.getWarnMax() != null && value.compareTo(thr.getWarnMax()) > 0) {
            level = "WARN";
            desc = sensorCode + " 超过预警上限 " + thr.getWarnMax() + "，当前值 " + value;
        } else if (thr.getWarnMin() != null && value.compareTo(thr.getWarnMin()) < 0) {
            level = "WARN";
            desc = sensorCode + " 低于预警下限 " + thr.getWarnMin() + "，当前值 " + value;
        }
        if (level == null) {
            return;
        }
        // 30 分钟内同设备同指标同级别不重复
        Calendar cal = Calendar.getInstance();
        cal.setTime(now);
        cal.add(Calendar.MINUTE, -30);
        Integer recent = alertsDao.selectCount(new QueryWrapper<Alerts>()
                .eq("device_id", device.getId())
                .eq("sensor_code", sensorCode)
                .eq("level", level)
                .gt("create_time", cal.getTime()));
        if (recent > 0) {
            return;
        }
        Alerts alert = new Alerts();
        alert.setDeviceId(device.getId());
        alert.setSensorCode(sensorCode);
        alert.setLevel(level);
        alert.setAlertValue(value);
        alert.setMessage(desc);
        alert.setStatus(0);
        alert.setCreateTime(now);
        alertsDao.insert(alert);
        // WebSocket 实时广播告警
        notifyWebSocketHandler.broadcast("{\"type\":\"alert\",\"alertId\":" + alert.getId()
                + ",\"level\":\"" + level + "\",\"sensorCode\":\"" + sensorCode + "\",\"alertValue\":"
                + value + ",\"message\":\"" + desc + "\"}");
    }
}