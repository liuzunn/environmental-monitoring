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
import org.nep.nepsystem.dao.SupervisionEventDao;
import org.nep.nepsystem.dao.InspectionTaskDao;
import org.nep.nepsystem.dao.GridsDao;
import org.nep.nepsystem.dao.RegionsDao;
import org.nep.nepsystem.bean.SupervisionEvent;
import org.nep.nepsystem.bean.InspectionTask;
import org.nep.nepsystem.bean.Grids;
import org.nep.nepsystem.bean.Regions;
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

    @Autowired
    private SupervisionEventDao supervisionEventDao;

    @Autowired
    private InspectionTaskDao inspectionTaskDao;

    @Autowired
    private GridsDao gridsDao;

    @Autowired
    private RegionsDao regionsDao;

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

        // Phase 8：真实业务监管统计（来源 supervision_event，字段追加兼容现有前端）
        java.util.Calendar cal = java.util.Calendar.getInstance();
        cal.set(java.util.Calendar.HOUR_OF_DAY, 0);
        cal.set(java.util.Calendar.MINUTE, 0);
        cal.set(java.util.Calendar.SECOND, 0);
        cal.set(java.util.Calendar.MILLISECOND, 0);
        Date today = cal.getTime();
        long totalEvents = supervisionEventDao.selectCount(null);
        long todayEvents = supervisionEventDao.selectCount(
                new QueryWrapper<SupervisionEvent>().ge("create_time", today));
        long pendingReview = supervisionEventDao.selectCount(
                new QueryWrapper<SupervisionEvent>().eq("status", "PENDING_REVIEW"));
        long processing = supervisionEventDao.selectCount(
                new QueryWrapper<SupervisionEvent>().in("status",
                        "APPROVED", "ASSIGNED", "ACCEPTED", "INSPECTING", "INSPECTED", "VERIFIED"));
        long closedEvents = supervisionEventDao.selectCount(
                new QueryWrapper<SupervisionEvent>().eq("status", "CLOSED"));
        int eventHandleRate = totalEvents == 0 ? 0 : (int) Math.round(closedEvents * 100.0 / totalEvents);
        m.put("todayEvents", todayEvents);
        m.put("pendingReview", pendingReview);
        m.put("processing", processing);
        m.put("closedEvents", closedEvents);
        m.put("eventHandleRate", eventHandleRate);
        m.put("totalEvents", totalEvents);
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

    /**
     * 业务监管统计聚合（Phase 8 新增，唯一新增统计接口；NEPV 展示真实业务监管数据）：
     * 数据来源 supervision_event / inspection_task / inspection_record / event_status_log
     */
    @GetMapping("/supervision")
    public Result<Map<String, Object>> supervisionStats() {
        Map<String, Object> out = new LinkedHashMap<>();

        // 区域事件数量（含未关联）
        List<Map<String, Object>> regionRows = supervisionEventDao.selectMaps(
                new QueryWrapper<SupervisionEvent>()
                        .select("region_id", "COUNT(*) AS cnt")
                        .groupBy("region_id"));
        List<Map<String, Object>> regionDistribution = new ArrayList<>();
        for (Map<String, Object> row : regionRows) {
            Map<String, Object> item = new LinkedHashMap<>();
            Object rid = row.get("region_id");
            String name = "未关联区域";
            if (rid != null) {
                Regions reg = regionsDao.selectById(((Number) rid).intValue());
                name = reg != null ? reg.getName() : "未知区域";
            }
            item.put("regionName", name);
            item.put("count", ((Number) row.get("cnt")).longValue());
            regionDistribution.add(item);
        }
        regionDistribution.sort((a, b) -> Long.compare((Long) b.get("count"), (Long) a.get("count")));
        out.put("regionDistribution", regionDistribution);

        // 污染类型分布
        List<Map<String, Object>> typeRows = supervisionEventDao.selectMaps(
                new QueryWrapper<SupervisionEvent>()
                        .select("event_type", "COUNT(*) AS cnt")
                        .groupBy("event_type"));
        List<Map<String, Object>> typeDistribution = new ArrayList<>();
        for (Map<String, Object> row : typeRows) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("eventType", String.valueOf(row.get("event_type")));
            item.put("count", ((Number) row.get("cnt")).longValue());
            typeDistribution.add(item);
        }
        typeDistribution.sort((a, b) -> Long.compare((Long) b.get("count"), (Long) a.get("count")));
        out.put("typeDistribution", typeDistribution);

        // 网格任务数量与完成率（CLOSED 计完成）
        long totalTasks = inspectionTaskDao.selectCount(null);
        long closedTasks = inspectionTaskDao.selectCount(
                new QueryWrapper<InspectionTask>().eq("status", "CLOSED"));
        Map<String, Object> taskStats = new LinkedHashMap<>();
        taskStats.put("totalTasks", totalTasks);
        taskStats.put("closedTasks", closedTasks);
        taskStats.put("completionRate", totalTasks == 0 ? 0 : (int) Math.round(closedTasks * 100.0 / totalTasks));
        out.put("taskStats", taskStats);
        // 按网格
        List<Map<String, Object>> gridRows = inspectionTaskDao.selectMaps(
                new QueryWrapper<InspectionTask>()
                        .select("grid_id", "COUNT(*) AS total",
                                "SUM(CASE WHEN status = 'CLOSED' THEN 1 ELSE 0 END) AS closed")
                        .isNotNull("grid_id")
                        .groupBy("grid_id"));
        List<Map<String, Object>> gridTasks = new ArrayList<>();
        for (Map<String, Object> row : gridRows) {
            Map<String, Object> item = new LinkedHashMap<>();
            Object gid = row.get("grid_id");
            String name = "未知网格";
            if (gid != null) {
                Grids g = gridsDao.selectById(((Number) gid).intValue());
                name = g != null ? g.getGridName() : name;
            }
            long total = ((Number) row.get("total")).longValue();
            long closed = row.get("closed") == null ? 0 : ((Number) row.get("closed")).longValue();
            item.put("gridName", name);
            item.put("totalTasks", total);
            item.put("closedTasks", closed);
            item.put("completionRate", total == 0 ? 0 : (int) Math.round(closed * 100.0 / total));
            gridTasks.add(item);
        }
        gridTasks.sort((a, b) -> Long.compare((Long) b.get("totalTasks"), (Long) a.get("totalTasks")));
        out.put("gridTasks", gridTasks);

        // 高风险事件：ALARM 级别且未关闭/未驳回，最新 10 条
        List<SupervisionEvent> highRisk = supervisionEventDao.selectList(
                new QueryWrapper<SupervisionEvent>()
                        .eq("level", "ALARM")
                        .notIn("status", "CLOSED", "REJECTED")
                        .orderByDesc("create_time")
                        .last("limit 10"));
        List<Map<String, Object>> highRiskEvents = new ArrayList<>();
        for (SupervisionEvent e : highRisk) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", e.getId().toString());
            item.put("eventNo", e.getEventNo());
            item.put("title", e.getTitle());
            item.put("level", e.getLevel());
            item.put("status", e.getStatus());
            item.put("createTime", e.getCreateTime());
            highRiskEvents.add(item);
        }
        out.put("highRiskEvents", highRiskEvents);

        // 事件趋势：近 7 天每日新增
        Date weekAgo = new Date(System.currentTimeMillis() - 7L * 24 * 3600 * 1000);
        List<Map<String, Object>> trendRows = supervisionEventDao.selectMaps(
                new QueryWrapper<SupervisionEvent>()
                        .select("DATE_FORMAT(create_time, '%Y-%m-%d') AS day", "COUNT(*) AS cnt")
                        .ge("create_time", weekAgo)
                        .groupBy("day")
                        .orderByAsc("day"));
        out.put("eventTrend", trendRows);

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