package org.nep.nepsystem.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.nep.nepsystem.bean.Alerts;
import org.nep.nepsystem.bean.DataQuality;
import org.nep.nepsystem.bean.Devices;
import org.nep.nepsystem.bean.MonitorData;
import org.nep.nepsystem.bean.Sensors;
import org.nep.nepsystem.config.SimulatorConfig;
import org.nep.nepsystem.dao.AlertsDao;
import org.nep.nepsystem.dao.DataQualityDao;
import org.nep.nepsystem.dao.DevicesDao;
import org.nep.nepsystem.dao.MonitorDataDao;
import org.nep.nepsystem.dao.SensorsDao;
import org.nep.nepsystem.dto.DeviceHealthDTO;
import org.nep.nepsystem.service.DeviceHealthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * 设备健康度实现（业务层升级，模块四）。
 * 指标定义（全部基于现有表实时计算）：
 *   在线率 onlineRate        = 近 7 天有上报数据的天数 / 7
 *   最近通信时间              = devices.last_report_time
 *   数据完整率 completeness   = 近 24h 已上报指标种类 / 该类型应上报指标种类（sensors 字典）
 *   异常次数 anomalyCount     = data_quality 中 ANOMALY 类别近 7 天仍活跃记录累计触发次数
 *   告警次数 alertCount       = alerts 近 7 天条数
 * 健康分 = 0.25*在线率 + 0.25*通信分 + 0.20*完整率 + 0.15*异常分 + 0.15*告警分
 */
@Service
public class DeviceHealthServiceImpl implements DeviceHealthService {

    private static final long DAY_MS = 24L * 3600 * 1000;
    private static final long WEEK_MS = 7L * DAY_MS;

    @Autowired private DevicesDao devicesDao;
    @Autowired private MonitorDataDao monitorDataDao;
    @Autowired private DataQualityDao dataQualityDao;
    @Autowired private AlertsDao alertsDao;
    @Autowired private SensorsDao sensorsDao;
    @Autowired private SimulatorConfig simulatorConfig;

    @Value("${health.report-window-days:7}")
    private int reportWindowDays;

    @Override
    public DeviceHealthDTO calculate(Integer deviceId) {
        Devices device = devicesDao.selectById(deviceId);
        if (device == null) {
            return null;
        }
        Date now = new Date();
        long windowMs = (long) reportWindowDays * DAY_MS;

        // 1) 在线率：近 7 天有数据的天数
        double onlineRate = 0;
        List<Map<String, Object>> days = monitorDataDao.selectMaps(new QueryWrapper<MonitorData>()
                .select("COUNT(DISTINCT DATE(report_time)) AS d")
                .eq("device_id", deviceId)
                .ge("report_time", new Date(now.getTime() - windowMs)));
        if (!days.isEmpty() && days.get(0).get("d") != null) {
            onlineRate = Math.min(100.0, ((Number) days.get(0).get("d")).doubleValue() / reportWindowDays * 100);
        }

        // 2) 最近通信时间
        Date lastCommunication = device.getLastReportTime();

        // 3) 数据完整率：近 24h 已上报指标 / 该类型应上报指标（字典）
        double completeness = 0;
        List<Map<String, Object>> seen = monitorDataDao.selectMaps(new QueryWrapper<MonitorData>()
                .select("COUNT(DISTINCT sensor_code) AS c")
                .eq("device_id", deviceId)
                .ge("report_time", new Date(now.getTime() - DAY_MS)));
        long seenCnt = (!seen.isEmpty() && seen.get(0).get("c") != null) ? ((Number) seen.get(0).get("c")).longValue() : 0;
        long expectedCnt = 0;
        if (device.getType() != null) {
            expectedCnt = sensorsDao.selectCount(new QueryWrapper<Sensors>()
                    .and(w -> w.eq("device_type", device.getType()).or().isNull("device_type")));
        }
        if (expectedCnt > 0) {
            completeness = Math.min(100.0, seenCnt * 100.0 / expectedCnt);
        }

        // 4) 异常次数：近 7 天仍活跃的 ANOMALY 记录累计触发次数
        Long anomalyCount = dataQualityDao.selectObjs(new QueryWrapper<DataQuality>()
                .select("COALESCE(SUM(occurrence_count), 0)")
                .eq("device_id", deviceId)
                .eq("category", "ANOMALY")
                .ge("last_seen", new Date(now.getTime() - WEEK_MS))).stream()
                .map(o -> Long.valueOf(String.valueOf(o))).findFirst().orElse(0L);

        // 5) 告警次数：近 7 天
        long alertCount = alertsDao.selectCount(new QueryWrapper<Alerts>()
                .eq("device_id", deviceId)
                .ge("create_time", new Date(now.getTime() - WEEK_MS)));

        // 6) 分项得分
        double communicationScore = communicationScore(lastCommunication, now);
        double anomalyScore = Math.max(0, 100 - anomalyCount * 20);
        double alertScore = Math.max(0, 100 - alertCount * 10);
        double health = Math.round((0.25 * onlineRate + 0.25 * communicationScore + 0.20 * completeness
                + 0.15 * anomalyScore + 0.15 * alertScore) * 10) / 10.0;

        DeviceHealthDTO dto = new DeviceHealthDTO();
        dto.setDeviceId(device.getId());
        dto.setDeviceCode(device.getDeviceCode());
        dto.setDeviceName(device.getDeviceName());
        dto.setType(device.getType());
        dto.setStatus(device.getStatus());
        dto.setHealthScore(health);
        dto.setHealthLevel(health >= 80 ? "HEALTHY" : (health >= 60 ? "FAIR" : "POOR"));
        dto.setOnlineRate(Math.round(onlineRate * 10) / 10.0);
        dto.setLastCommunication(lastCommunication);
        dto.setDataCompleteness(Math.round(completeness * 10) / 10.0);
        dto.setAnomalyCount(anomalyCount);
        dto.setAlertCount(alertCount);
        return dto;
    }

    /** 通信时效分：3x 上报周期内 100 分，1 小时内 70 分，24 小时内 40 分，否则 0 分 */
    private double communicationScore(Date lastCommunication, Date now) {
        if (lastCommunication == null) {
            return 0;
        }
        long age = now.getTime() - lastCommunication.getTime();
        if (age <= 3L * simulatorConfig.getIntervalMs()) return 100;
        if (age <= 3600 * 1000L) return 70;
        if (age <= DAY_MS) return 40;
        return 0;
    }

    @Override
    public List<DeviceHealthDTO> calculateAll() {
        List<Devices> devices = devicesDao.selectList(new QueryWrapper<Devices>().orderByAsc("id"));
        List<DeviceHealthDTO> out = new ArrayList<>();
        for (Devices d : devices) {
            DeviceHealthDTO dto = calculate(d.getId());
            if (dto != null) {
                out.add(dto);
            }
        }
        return out;
    }
}
