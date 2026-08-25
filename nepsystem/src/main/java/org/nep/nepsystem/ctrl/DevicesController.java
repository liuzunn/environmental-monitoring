package org.nep.nepsystem.ctrl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.nep.nepsystem.bean.Devices;
import org.nep.nepsystem.bean.MonitorData;
import org.nep.nepsystem.common.PageResult;
import org.nep.nepsystem.common.Result;
import org.nep.nepsystem.dao.DevicesDao;
import org.nep.nepsystem.dao.MonitorDataDao;
import org.nep.nepsystem.exception.BizException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 设备管理接口
 */
@RestController
@RequestMapping("/api/devices")
public class DevicesController {

    @Autowired
    private DevicesDao devicesDao;

    @Autowired
    private MonitorDataDao monitorDataDao;

    /** 分页查询：支持关键字/类型/状态过滤 */
    @GetMapping("/page")
    public Result<PageResult<Devices>> page(@RequestParam(defaultValue = "1") int page,
                                            @RequestParam(defaultValue = "10") int size,
                                            @RequestParam(required = false) String keyword,
                                            @RequestParam(required = false) String type,
                                            @RequestParam(required = false) Integer status) {
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
        qw.orderByDesc("create_time");
        IPage<Devices> p = devicesDao.selectPage(new Page<>(page, size), qw);
        return Result.ok(PageResult.of(p));
    }

    /** 新增设备（device_code 唯一校验） */
    @PostMapping
    public Result<Void> add(@RequestBody Devices device) {
        if (!StringUtils.hasText(device.getDeviceCode()) || !StringUtils.hasText(device.getDeviceName())) {
            throw new BizException(400, "设备编号和名称不能为空");
        }
        Integer cnt = devicesDao.selectCount(new QueryWrapper<Devices>().eq("device_code", device.getDeviceCode()));
        if (cnt > 0) {
            throw new BizException("设备编号已存在: " + device.getDeviceCode());
        }
        if (device.getStatus() == null) {
            device.setStatus(0);
        }
        devicesDao.insert(device);
        return Result.ok("新增成功", null);
    }

    /** 修改设备 */
    @PutMapping
    public Result<Void> update(@RequestBody Devices device) {
        if (device.getId() == null) {
            throw new BizException(400, "缺少设备ID");
        }
        devicesDao.updateById(device);
        return Result.ok("修改成功", null);
    }

    /** 删除设备 */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Integer id) {
        // 有关联监测数据时不允许删除（课程设计：直接清掉关联数据更简单，这里选择校验提示）
        Integer cnt = monitorDataDao.selectCount(new QueryWrapper<MonitorData>().eq("device_id", id));
        if (cnt > 0) {
            throw new BizException("该设备存在 " + cnt + " 条监测数据，请先清理数据再删除");
        }
        devicesDao.deleteById(id);
        return Result.ok("删除成功", null);
    }

    /** 在线设备数 */
    @GetMapping("/online/count")
    public Result<Map<String, Object>> onlineCount() {
        Integer total = devicesDao.selectCount(null);
        Integer online = devicesDao.selectCount(new QueryWrapper<Devices>().eq("status", 1));
        Map<String, Object> m = new HashMap<>();
        m.put("total", total);
        m.put("online", online);
        return Result.ok(m);
    }

    /** 设备最新一次各指标数据 */
    @GetMapping("/{id}/latest")
    public Result<Map<String, Object>> latest(@PathVariable Integer id) {
        Devices device = devicesDao.selectById(id);
        if (device == null) {
            throw new BizException("设备不存在: " + id);
        }
        // 取该设备所有指标的最新一条
        List<Object> codes = monitorDataDao.selectObjs(
                new QueryWrapper<MonitorData>().select("DISTINCT sensor_code").eq("device_id", id));
        Map<String, Object> data = new HashMap<>();
        data.put("device", device);
        Map<String, Object> values = new HashMap<>();
        for (Object code : codes) {
            MonitorData md = monitorDataDao.selectOne(new QueryWrapper<MonitorData>()
                    .eq("device_id", id).eq("sensor_code", code.toString())
                    .orderByDesc("report_time").last("limit 1"));
            if (md != null) {
                Map<String, Object> item = new HashMap<>();
                item.put("value", md.getValue());
                item.put("reportTime", md.getReportTime());
                values.put(code.toString(), item);
            }
        }
        data.put("values", values);
        return Result.ok(data);
    }
}