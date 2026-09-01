package org.nep.nepsystem.ctrl;

import org.nep.nepsystem.common.Result;
import org.nep.nepsystem.dto.DeviceHealthDTO;
import org.nep.nepsystem.exception.BizException;
import org.nep.nepsystem.service.DeviceHealthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 设备健康度接口（业务层升级新增）：
 * GET /api/health/devices        - 全部设备健康度
 * GET /api/health/devices/{id}   - 单台设备健康度
 */
@RestController
@RequestMapping("/api/health")
public class DeviceHealthController {

    @Autowired
    private DeviceHealthService deviceHealthService;

    @GetMapping("/devices")
    public Result<List<DeviceHealthDTO>> devices() {
        return Result.ok(deviceHealthService.calculateAll());
    }

    @GetMapping("/devices/{id}")
    public Result<DeviceHealthDTO> device(@PathVariable Integer id) {
        DeviceHealthDTO dto = deviceHealthService.calculate(id);
        if (dto == null) {
            throw new BizException(400, "设备不存在: " + id);
        }
        return Result.ok(dto);
    }
}
