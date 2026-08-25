package org.nep.nepsystem.ctrl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.nep.nepsystem.bean.MonitorData;
import org.nep.nepsystem.common.PageResult;
import org.nep.nepsystem.common.Result;
import org.nep.nepsystem.dao.MonitorDataDao;
import org.nep.nepsystem.service.MonitorDataService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * 监测数据接口：上报 / 最新 / 历史 / 趋势 / 导出
 */
@RestController
@RequestMapping("/api/data")
public class DataController {

    @Autowired
    private MonitorDataDao monitorDataDao;

    @Autowired
    private MonitorDataService monitorDataService;

    /** 数据上报（模拟器与真实硬件共用）：{deviceCode, items:[{sensorCode,value}], reportTime} */
    @PostMapping("/report")
    public Result<Void> report(@RequestBody Map<String, Object> body) {
        String deviceCode = (String) body.get("deviceCode");
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> items = (List<Map<String, Object>>) body.get("items");
        Date reportTime = null;
        Object rt = body.get("reportTime");
        if (rt != null) {
            try {
                reportTime = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse(rt.toString());
            } catch (Exception e) {
                reportTime = new Date();
            }
        }
        monitorDataService.report(deviceCode, items, reportTime);
        return Result.ok("上报成功", null);
    }

    /** 最新数据：指定设备与指标（逗号分隔） */
    @GetMapping("/latest")
    public Result<Map<String, Object>> latest(@RequestParam Integer deviceId,
                                              @RequestParam(required = false) String sensorCodes) {
        QueryWrapper<MonitorData> qw = new QueryWrapper<MonitorData>().eq("device_id", deviceId);
        if (StringUtils.hasText(sensorCodes)) {
            qw.in("sensor_code", (Object[]) sensorCodes.split(","));
        }
        qw.orderByDesc("report_time").last("limit 50");
        List<MonitorData> list = monitorDataDao.selectList(qw);
        Map<String, Object> result = new java.util.LinkedHashMap<>();
        for (MonitorData md : list) {
            Map<String, Object> item = new java.util.HashMap<>();
            item.put("value", md.getValue());
            item.put("reportTime", md.getReportTime());
            result.putIfAbsent(md.getSensorCode(), item);
        }
        return Result.ok(result);
    }

    /** 历史分页：时间倒序 */
    @GetMapping("/history")
    public Result<PageResult<MonitorData>> history(@RequestParam(required = false) Integer deviceId,
                                                   @RequestParam(required = false) String sensorCode,
                                                   @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") Date start,
                                                   @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") Date end,
                                                   @RequestParam(defaultValue = "1") int page,
                                                   @RequestParam(defaultValue = "20") int size) {
        QueryWrapper<MonitorData> qw = new QueryWrapper<>();
        if (deviceId != null) qw.eq("device_id", deviceId);
        if (StringUtils.hasText(sensorCode)) qw.eq("sensor_code", sensorCode);
        if (start != null) qw.ge("report_time", start);
        if (end != null) qw.le("report_time", end);
        qw.orderByDesc("report_time");
        IPage<MonitorData> p = monitorDataDao.selectPage(new Page<>(page, size), qw);
        return Result.ok(PageResult.of(p));
    }

    /** 趋势聚合：按小时/天求平均 */
    @GetMapping("/trend")
    public Result<List<Map<String, Object>>> trend(@RequestParam Integer deviceId,
                                                   @RequestParam String sensorCode,
                                                   @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") Date start,
                                                   @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") Date end,
                                                   @RequestParam(defaultValue = "hour") String interval) {
        String fmt = "hour".equals(interval) ? "%Y-%m-%d %H:00" : "%Y-%m-%d";
        List<Map<String, Object>> rows = monitorDataDao.selectMaps(new QueryWrapper<MonitorData>()
                .select("DATE_FORMAT(report_time, '" + fmt + "') AS t", "AVG(value) AS avg_value", "MAX(value) AS max_value", "MIN(value) AS min_value")
                .eq("device_id", deviceId)
                .eq("sensor_code", sensorCode)
                .ge(start != null, "report_time", start)
                .le(end != null, "report_time", end)
                .groupBy("t")
                .orderByAsc("t"));
        return Result.ok(rows);
    }

    /** CSV 导出 */
    @GetMapping("/export")
    public void export(@RequestParam(required = false) Integer deviceId,
                       @RequestParam(required = false) String sensorCode,
                       @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") Date start,
                       @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") Date end,
                       HttpServletResponse response) throws IOException {
        QueryWrapper<MonitorData> qw = new QueryWrapper<>();
        if (deviceId != null) qw.eq("device_id", deviceId);
        if (StringUtils.hasText(sensorCode)) qw.eq("sensor_code", sensorCode);
        if (start != null) qw.ge("report_time", start);
        if (end != null) qw.le("report_time", end);
        qw.orderByAsc("report_time").last("limit 10000");
        List<MonitorData> list = monitorDataDao.selectList(qw);

        response.setContentType("text/csv;charset=UTF-8");
        response.setHeader("Content-Disposition", "attachment;filename=monitor_data.csv");
        PrintWriter w = new PrintWriter(new OutputStreamWriter(response.getOutputStream(), StandardCharsets.UTF_8));
        w.write("\uFEFF"); // BOM 让 Excel 正确识别 UTF-8
        w.println("device_id,sensor_code,value,report_time");
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        for (MonitorData md : list) {
            w.println(md.getDeviceId() + "," + md.getSensorCode() + "," + md.getValue() + "," + sdf.format(md.getReportTime()));
        }
        w.flush();
        w.close();
    }
}
