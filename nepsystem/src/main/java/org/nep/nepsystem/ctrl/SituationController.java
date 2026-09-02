package org.nep.nepsystem.ctrl;

import org.nep.nepsystem.common.Result;
import org.nep.nepsystem.dto.DeviceSituationDTO;
import org.nep.nepsystem.dto.SituationOverviewDTO;
import org.nep.nepsystem.service.SituationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 空间态势接口（空间态势升级新增）：
 * GET /api/situation/overview  - 态势总览（含环境状态 healthy）
 * GET /api/situation/devices   - 态势设备列表（keyword/type/status/alertLevel 过滤）
 */
@RestController
@RequestMapping("/api/situation")
public class SituationController {

    @Autowired
    private SituationService situationService;

    @GetMapping("/overview")
    public Result<SituationOverviewDTO> overview() {
        return Result.ok(situationService.overview());
    }

    @GetMapping("/devices")
    public Result<List<DeviceSituationDTO>> devices(@RequestParam(required = false) String keyword,
                                                    @RequestParam(required = false) String type,
                                                    @RequestParam(required = false) Integer status,
                                                    @RequestParam(required = false) String alertLevel) {
        return Result.ok(situationService.devices(keyword, type, status, alertLevel));
    }
}
