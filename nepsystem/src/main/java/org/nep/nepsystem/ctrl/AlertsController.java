package org.nep.nepsystem.ctrl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.nep.nepsystem.bean.Alerts;
import org.nep.nepsystem.common.PageResult;
import org.nep.nepsystem.common.Result;
import org.nep.nepsystem.dao.AlertsDao;
import org.nep.nepsystem.exception.BizException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 告警接口：查询/处理/未处理数/近7天统计
 */
@RestController
@RequestMapping("/api/alerts")
public class AlertsController {

    @Autowired
    private AlertsDao alertsDao;

    /** 分页查询：级别/状态/设备过滤 */
    @GetMapping("/page")
    public Result<PageResult<Alerts>> page(@RequestParam(defaultValue = "1") int page,
                                           @RequestParam(defaultValue = "10") int size,
                                           @RequestParam(required = false) String level,
                                           @RequestParam(required = false) Integer status,
                                           @RequestParam(required = false) Integer deviceId) {
        QueryWrapper<Alerts> qw = new QueryWrapper<>();
        if (StringUtils.hasText(level)) qw.eq("level", level);
        if (status != null) qw.eq("status", status);
        if (deviceId != null) qw.eq("device_id", deviceId);
        qw.orderByDesc("create_time");
        IPage<Alerts> p = alertsDao.selectPage(new Page<>(page, size), qw);
        return Result.ok(PageResult.of(p));
    }

    /** 处理告警 */
    @PutMapping("/{id}/handle")
    public Result<Void> handle(@PathVariable Long id, @RequestParam(required = false) String handleUser) {
        Alerts alert = alertsDao.selectById(id);
        if (alert == null) {
            throw new BizException("告警不存在: " + id);
        }
        alert.setStatus(1);
        alert.setHandleUser(StringUtils.hasText(handleUser) ? handleUser : "admin");
        alert.setHandleTime(new Date());
        alertsDao.updateById(alert);
        return Result.ok("已处理", null);
    }

    /** 未处理告警数 + 最新5条 */
    @GetMapping("/unhandled")
    public Result<Map<String, Object>> unhandled() {
        Integer count = alertsDao.selectCount(new QueryWrapper<Alerts>().eq("status", 0));
        List<Alerts> latest = alertsDao.selectList(new QueryWrapper<Alerts>()
                .eq("status", 0).orderByDesc("create_time").last("limit 5"));
        Map<String, Object> m = new HashMap<>();
        m.put("count", count);
        m.put("latest", latest);
        return Result.ok(m);
    }

    /** 近7天每日告警数（按级别分组） */
    @GetMapping("/stat")
    public Result<List<Map<String, Object>>> stat() {
        // 近 7 天按天、按级别统计
        List<Map<String, Object>> rows = alertsDao.selectMaps(new QueryWrapper<Alerts>()
                .select("DATE_FORMAT(create_time, '%Y-%m-%d') AS day", "level", "COUNT(*) AS cnt")
                .ge("create_time", new Date(System.currentTimeMillis() - 7L * 24 * 3600 * 1000))
                .groupBy("day", "level")
                .orderByAsc("day"));
        return Result.ok(rows);
    }
}