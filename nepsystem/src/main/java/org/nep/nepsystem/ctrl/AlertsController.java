package org.nep.nepsystem.ctrl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.nep.nepsystem.bean.Alerts;
import org.nep.nepsystem.common.PageResult;
import org.nep.nepsystem.common.Result;
import org.nep.nepsystem.dto.AlertActionDTO;
import org.nep.nepsystem.dao.AlertsDao;
import org.nep.nepsystem.exception.BizException;
import org.nep.nepsystem.service.AlertLifecycleService;
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

    @Autowired
    private AlertLifecycleService alertLifecycleService;

    /** 分页查询：级别/状态/设备过滤 */
    @GetMapping("/page")
    public Result<PageResult<Alerts>> page(@RequestParam(defaultValue = "1") int page,
                                           @RequestParam(defaultValue = "10") int size,
                                           @RequestParam(required = false) String level,
                                           @RequestParam(required = false) Integer status,
                                           @RequestParam(required = false) Integer deviceId,
                                           @RequestParam(required = false) String state) {
        QueryWrapper<Alerts> qw = new QueryWrapper<>();
        if (StringUtils.hasText(level)) qw.eq("level", level);
        if (status != null) qw.eq("status", status);
        if (deviceId != null) qw.eq("device_id", deviceId);
        if (StringUtils.hasText(state)) qw.eq("state", state);
        qw.orderByDesc("create_time");
        IPage<Alerts> p = alertsDao.selectPage(new Page<>(page, size), qw);
        return Result.ok(PageResult.of(p));
    }

    /** 处理告警（旧接口，兼容保留：等价于解决，state -> RESOLVED，status=1） */
    @PutMapping("/{id}/handle")
    public Result<Void> handle(@PathVariable Long id, @RequestParam(required = false) String handleUser) {
        alertLifecycleService.resolve(id, handleUser);
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

    /** 确认告警：WARN/ALARM -> ACKNOWLEDGED（业务层升级新增） */
    @PutMapping("/{id}/acknowledge")
    public Result<Alerts> acknowledge(@PathVariable Long id,
                                      @RequestBody(required = false) AlertActionDTO body,
                                      @RequestParam(required = false) String ackUser) {
        String user = body != null ? body.getUser() : null;
        if (user == null || user.isEmpty()) user = ackUser;
        return Result.ok("已确认", alertLifecycleService.acknowledge(id, user));
    }

    /** 开始处理：-> PROCESSING（业务层升级新增） */
    @PutMapping("/{id}/process")
    public Result<Alerts> process(@PathVariable Long id,
                                  @RequestBody(required = false) AlertActionDTO body,
                                  @RequestParam(required = false) String handleUser) {
        String user = body != null ? body.getUser() : null;
        if (user == null || user.isEmpty()) user = handleUser;
        return Result.ok("处理中", alertLifecycleService.process(id, user));
    }

    /** 解决告警：-> RESOLVED（业务层升级新增） */
    @PutMapping("/{id}/resolve")
    public Result<Alerts> resolve(@PathVariable Long id,
                                  @RequestBody(required = false) AlertActionDTO body,
                                  @RequestParam(required = false) String resolveUser) {
        String user = body != null ? body.getUser() : null;
        if (user == null || user.isEmpty()) user = resolveUser;
        return Result.ok("已解决", alertLifecycleService.resolve(id, user));
    }
}