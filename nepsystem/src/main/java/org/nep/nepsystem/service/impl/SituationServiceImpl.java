package org.nep.nepsystem.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.nep.nepsystem.bean.Alerts;
import org.nep.nepsystem.bean.Devices;
import org.nep.nepsystem.bean.MonitorData;
import org.nep.nepsystem.dao.AlertsDao;
import org.nep.nepsystem.dao.DevicesDao;
import org.nep.nepsystem.dao.MonitorDataDao;
import org.nep.nepsystem.dto.DeviceSituationDTO;
import org.nep.nepsystem.dto.SituationOverviewDTO;
import org.nep.nepsystem.service.DataQualityService;
import org.nep.nepsystem.service.DeviceHealthService;
import org.nep.nepsystem.service.SituationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 空间态势实现（空间态势升级新增）。
 * 活跃告警口径：status=0 且 state IN (WARN/ALARM/ACKNOWLEDGED/PROCESSING)。
 */
@Service
public class SituationServiceImpl implements SituationService {

    private static final List<String> OPEN_STATES = Arrays.asList("WARN", "ALARM", "ACKNOWLEDGED", "PROCESSING");

    @Autowired private DevicesDao devicesDao;
    @Autowired private MonitorDataDao monitorDataDao;
    @Autowired private AlertsDao alertsDao;
    @Autowired private DeviceHealthService deviceHealthService;
    @Autowired private DataQualityService dataQualityService;

    /** 活跃告警: deviceId -> 告警行（含 level/sensorCode/message/alertValue/createTime） */
    private Map<Integer, List<Map<String, Object>>> activeAlertGroups() {
        List<Map<String, Object>> rows = alertsDao.selectMaps(new QueryWrapper<Alerts>()
                .select("device_id", "id", "level", "state", "sensor_code", "message", "alert_value", "create_time")
                .eq("status", 0)
                .in("state", OPEN_STATES)
                .orderByDesc("create_time"));
        Map<Integer, List<Map<String, Object>>> groups = new HashMap<>();
        for (Map<String, Object> r : rows) {
            Object did = r.get("device_id");
            if (did == null) continue;
            int deviceId = ((Number) did).intValue();
            groups.computeIfAbsent(deviceId, k -> new ArrayList<>()).add(r);
        }
        return groups;
    }

    private Set<Integer> deviceIdsWithLevel(Map<Integer, List<Map<String, Object>>> groups, String level) {
        return groups.entrySet().stream()
                .filter(e -> e.getValue().stream().anyMatch(a -> level.equals(a.get("level"))))
                .map(Map.Entry::getKey)
                .collect(Collectors.toSet());
    }

    @Override
    public SituationOverviewDTO overview() {
        Map<Integer, List<Map<String, Object>>> groups = activeAlertGroups();
        SituationOverviewDTO dto = new SituationOverviewDTO();
        long total = devicesDao.selectCount(null);
        long online = devicesDao.selectCount(new QueryWrapper<Devices>().eq("status", 1));
        dto.setTotalDevices(total);
        dto.setOnlineDevices(online);
        dto.setOfflineDevices(Math.max(0, total - online));
        dto.setWarnDevices((long) deviceIdsWithLevel(groups, "WARN").size());
        dto.setAlarmDevices((long) deviceIdsWithLevel(groups, "ALARM").size());
        long activeAlerts = groups.values().stream().mapToLong(List::size).sum();
        dto.setActiveAlerts(activeAlerts);
        dto.setHealthy(activeAlerts == 0);
        return dto;
    }

    @Override
    public List<DeviceSituationDTO> devices(String keyword, String type, Integer status, String alertLevel) {
        Map<Integer, List<Map<String, Object>>> groups = activeAlertGroups();
        Set<Integer> warnIds = deviceIdsWithLevel(groups, "WARN");
        Set<Integer> alarmIds = deviceIdsWithLevel(groups, "ALARM");

        QueryWrapper<Devices> qw = new QueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            qw.and(w -> w.like("device_code", keyword).or().like("device_name", keyword));
        }
        if (StringUtils.hasText(type)) {
            qw.eq("type", type);
        }
        if (status != null) {
            qw.eq("status", status);
        }
        if ("WARN".equals(alertLevel)) {
            // 空集合 in() 会生成非法 SQL，直接返回空结果（语义：没有预警设备）
            if (warnIds.isEmpty()) {
                return new ArrayList<>();
            }
            qw.in("id", warnIds);
        } else if ("ALARM".equals(alertLevel)) {
            if (alarmIds.isEmpty()) {
                return new ArrayList<>();
            }
            qw.in("id", alarmIds);
        } else if ("NONE".equals(alertLevel)) {
            // 无告警设备
            Set<Integer> all = new HashSet<>(warnIds);
            all.addAll(alarmIds);
            if (!all.isEmpty()) {
                qw.notIn("id", all);
            }
        }
        qw.orderByAsc("id");
        List<Devices> devices = devicesDao.selectList(qw);

        List<DeviceSituationDTO> out = new ArrayList<>();
        for (Devices d : devices) {
            DeviceSituationDTO dto = new DeviceSituationDTO();
            dto.setDeviceId(d.getId());
            dto.setDeviceCode(d.getDeviceCode());
            dto.setDeviceName(d.getDeviceName());
            dto.setType(d.getType());
            dto.setLocation(d.getLocation());
            dto.setLat(d.getLat());
            dto.setLng(d.getLng());
            dto.setStatus(d.getStatus());
            dto.setOnline(d.getStatus() != null && d.getStatus() == 1);
            List<Map<String, Object>> alerts = groups.getOrDefault(d.getId(), Collections.emptyList());
            dto.setAlerts(alerts);
            dto.setWarn(alerts.stream().anyMatch(a -> "WARN".equals(a.get("level"))));
            dto.setAlarm(alerts.stream().anyMatch(a -> "ALARM".equals(a.get("level"))));
            dto.setValues(latestValues(d.getId()));
            org.nep.nepsystem.dto.DeviceHealthDTO health = deviceHealthService.calculate(d.getId());
            if (health != null) {
                dto.setHealthScore(health.getHealthScore());
                dto.setHealthLevel(health.getHealthLevel());
            }
            dto.setQualityStatus(dataQualityService.resolveStatus(d.getId()));
            out.add(dto);
        }
        return out;
    }

    /** 单查询取每指标最新一条（按 report_time 倒序，首现即最新） */
    private Map<String, Map<String, Object>> latestValues(Integer deviceId) {
        List<Map<String, Object>> rows = monitorDataDao.selectMaps(new QueryWrapper<MonitorData>()
                .select("sensor_code", "value", "report_time")
                .eq("device_id", deviceId)
                .orderByDesc("report_time")
                .last("limit 100"));
        Map<String, Map<String, Object>> values = new LinkedHashMap<>();
        for (Map<String, Object> r : rows) {
            Object code = r.get("sensor_code");
            if (code == null || values.containsKey(code.toString())) {
                continue;
            }
            Map<String, Object> item = new HashMap<>();
            item.put("value", r.get("value"));
            item.put("reportTime", r.get("report_time"));
            values.put(code.toString(), item);
        }
        return values;
    }
}
