package org.nep.nepsystem.ctrl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.nep.nepsystem.bean.Thresholds;
import org.nep.nepsystem.common.Result;
import org.nep.nepsystem.dao.ThresholdsDao;
import org.nep.nepsystem.exception.BizException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 告警阈值接口：CRUD（deviceId 为空表示全局默认阈值）
 */
@RestController
@RequestMapping("/api/thresholds")
public class ThresholdsController {

    @Autowired
    private ThresholdsDao thresholdsDao;

    /** 列表：可按设备过滤 */
    @GetMapping
    public Result<List<Thresholds>> list(@RequestParam(required = false) Integer deviceId) {
        QueryWrapper<Thresholds> qw = new QueryWrapper<>();
        if (deviceId != null) {
            qw.eq("device_id", deviceId);
        }
        qw.orderByDesc("update_time");
        return Result.ok(thresholdsDao.selectList(qw));
    }

    /** 新增 */
    @PostMapping
    public Result<Void> add(@RequestBody Thresholds thr) {
        if (!StringUtils.hasText(thr.getSensorCode())) {
            throw new BizException(400, "指标编码不能为空");
        }
        if (thr.getEnabled() == null) {
            thr.setEnabled(1);
        }
        thr.setId(null);
        thresholdsDao.insert(thr);
        return Result.ok("新增成功", null);
    }

    /** 修改 */
    @PutMapping
    public Result<Void> update(@RequestBody Thresholds thr) {
        if (thr.getId() == null) {
            throw new BizException(400, "缺少阈值ID");
        }
        thresholdsDao.updateById(thr);
        return Result.ok("修改成功", null);
    }

    /** 删除 */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Integer id) {
        thresholdsDao.deleteById(id);
        return Result.ok("删除成功", null);
    }
}
