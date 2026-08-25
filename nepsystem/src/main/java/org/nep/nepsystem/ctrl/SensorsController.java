package org.nep.nepsystem.ctrl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.nep.nepsystem.bean.Sensors;
import org.nep.nepsystem.common.Result;
import org.nep.nepsystem.dao.SensorsDao;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 监测指标字典接口
 */
@RestController
@RequestMapping("/api/sensors")
public class SensorsController {

    @Autowired
    private SensorsDao sensorsDao;

    /** 指标字典列表，可按设备类型过滤 */
    @GetMapping
    public Result<List<Sensors>> list(@RequestParam(required = false) String deviceType) {
        QueryWrapper<Sensors> qw = new QueryWrapper<>();
        if (deviceType != null && !deviceType.isEmpty()) {
            qw.and(w -> w.eq("device_type", deviceType).or().isNull("device_type"));
        }
        qw.orderByAsc("id");
        return Result.ok(sensorsDao.selectList(qw));
    }
}
