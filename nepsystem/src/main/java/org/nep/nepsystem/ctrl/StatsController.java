package org.nep.nepsystem.ctrl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.nep.nepsystem.bean.Alerts;
import org.nep.nepsystem.bean.Devices;
import org.nep.nepsystem.bean.MonitorData;
import org.nep.nepsystem.bean.Sensors;
import org.nep.nepsystem.common.Result;
import org.nep.nepsystem.dao.AlertsDao;
import org.nep.nepsystem.dao.DevicesDao;
import org.nep.nepsystem.dao.MonitorDataDao;
import org.nep.nepsystem.dao.SensorsDao;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.*;

/**
 * 统计接口：总览 / 环境质量评分 / 设备上报排行
 */
@RestController
@RequestMapping("/api/stats")
public class StatsController {

    @Autowired
    private DevicesDao devicesDao;

    @Autowired
    private MonitorDataDao monitorDataDao;

    @Autowired
    private AlertsDao alertsDao;

    @Autowired
    private SensorsDao sensorsDao;

    /** 总览：设备总数/在线数/今日上报条数/未处理告警数 */
    @GetMapping("/overview")
    public Result<Map<String, Object>> overview() {
        Integer totalDevices = devicesDao.selectCount(null);
        Integer onlineDevices = devicesDao.selectCount(new QueryWrapper<Devices>().eq("status", 1));
        Integer todayCount = monitorDataDao.selectCount(new QueryWrapper<MonitorData>()
                .ge("report_time", new Date(System.currentTimeMillis() - 24L * 3600 * 1000)));
        Integer unhandled = alertsDao.selectCount(new QueryWrapper<Alerts>().eq("status", 0));
        Map<String, Object> m = new HashMap<>();
        m.put("totalDevices", totalDevices);
        m.put("onlineDevices", onlineDevices);
        m.put("todayReports", todayCount);
        m.put("unhandledAlerts", unhandled);
        return Result.ok(m);
    }

    /**
     * 环境质量评分：参照 AQI 思路，指标值 / standard_max 计算得分，0-100 分制
     * value > standard_max 视为超标（得分 >100，按 100 截断显示为超标）
     */
    @GetMapping("/quality")
    public Result<Map<String, Object>> quality(@RequestParam(required = false) Integer deviceId,
                                               @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") Date start,
                                               @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") Date end) {
        // 该设备最近一次各指标值
        List<Map<String, Object>> rows = monitorDataDao.selectMaps(new QueryWrapper<MonitorData>()
                .select("sensor_code", "value")
                .eq(deviceId != null, "device_id", deviceId)
                .ge(start != null, "report_time", start)
                .le(end != null, "report_time", end)
                .orderByDesc("report_time")
                .last("limit 100"));
        Map<String, BigDecimal> latest = new LinkedHashMap<>();
        for (Map<String, Object> row : rows) {
            String code = String.valueOf(row.get("sensor_code"));
            if (!latest.containsKey(code)) {
                latest.put(code, new BigDecimal(String.valueOf(row.get("value"))));
            }
        }
        Map<String, Object> result = new LinkedHashMap<>();
        double sum = 0;
        int cnt = 0;
        for (Map.Entry<String, BigDecimal> e : latest.entrySet()) {
            Sensors sensor = sensorsDao.selectOne(new QueryWrapper<Sensors>().eq("sensor_code", e.getKey()).last("limit 1"));
            BigDecimal std = sensor != null ? sensor.getStandardMax() : null;
            double score = 100;
            boolean over = false;
            if (std != null && std.compareTo(BigDecimal.ZERO) > 0) {
                score = e.getValue().doubleValue() / std.doubleValue() * 100;
                over = score > 100;
                score = Math.min(score, 100);
                score = Math.round(score * 10) / 10.0;
            }
            Map<String, Object> item = new HashMap<>();
            item.put("score", score);
            item.put("over", over);
            item.put("standard", std);
            item.put("unit", sensor != null ? sensor.getUnit() : null);
            result.put(e.getKey(), item);
            sum += score;
            cnt++;
        }
        Map<String, Object> out = new HashMap<>();
        out.put("scores", result);
        out.put("overall", cnt > 0 ? Math.round(sum / cnt * 10) / 10.0 : null);
        return Result.ok(out);
    }

    /** 设备上报量排行 Top10 */
    @GetMapping("/device-ranking")
    public Result<List<Map<String, Object>>> deviceRanking(@RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") Date start,
                                                           @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") Date end) {
        QueryWrapper<MonitorData> qw = new QueryWrapper<MonitorData>()
                .select("device_id", "COUNT(*) AS cnt")
                .ge(start != null, "report_time", start)
                .le(end != null, "report_time", end)
                .groupBy("device_id")
                .orderByDesc("cnt")
                .last("limit 10");
        List<Map<String, Object>> rows = monitorDataDao.selectMaps(qw);
        for (Map<String, Object> row : rows) {
            Object did = row.get("device_id");
            if (did != null) {
                Devices d = devicesDao.selectById(Integer.parseInt(did.toString()));
                row.put("deviceName", d != null ? d.getDeviceName() : "未知设备");
                row.put("deviceCode", d != null ? d.getDeviceCode() : "");
            }
        }
        return Result.ok(rows);
    }
}