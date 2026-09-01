package org.nep.nepsystem.ctrl;

import org.nep.nepsystem.bean.DataQuality;
import org.nep.nepsystem.bean.Devices;
import org.nep.nepsystem.common.Result;
import org.nep.nepsystem.dao.DevicesDao;
import org.nep.nepsystem.dto.QualityIssueDTO;
import org.nep.nepsystem.dto.QualityStatusDTO;
import org.nep.nepsystem.service.DataQualityService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 数据质量检测接口（业务层升级新增）：
 * GET /api/quality/issues  - 质量/异常问题列表（可按设备/类别/问题类型过滤）
 * GET /api/quality/status  - 设备质量状态 GOOD/WARNING/BAD
 */
@RestController
@RequestMapping("/api/quality")
public class DataQualityController {

    @Autowired
    private DataQualityService dataQualityService;

    @Autowired
    private DevicesDao devicesDao;

    @GetMapping("/issues")
    public Result<List<QualityIssueDTO>> issues(@RequestParam(required = false) Integer deviceId,
                                                @RequestParam(required = false) String category,
                                                @RequestParam(required = false) String issueType,
                                                @RequestParam(defaultValue = "50") int limit) {
        List<DataQuality> rows = dataQualityService.queryIssues(deviceId, category, issueType, limit);
        // 批量取设备名（避免逐行查询）
        List<Integer> devIds = rows.stream().map(DataQuality::getDeviceId).distinct().collect(Collectors.toList());
        Map<Integer, String> names = devIds.isEmpty() ? java.util.Collections.emptyMap() :
                devicesDao.selectBatchIds(devIds).stream()
                        .collect(Collectors.toMap(Devices::getId, d -> d.getDeviceName() == null ? "" : d.getDeviceName(), (a, b) -> a));
        List<QualityIssueDTO> out = new ArrayList<>();
        for (DataQuality dq : rows) {
            QualityIssueDTO dto = new QualityIssueDTO();
            dto.setId(dq.getId());
            dto.setDeviceId(dq.getDeviceId());
            dto.setDeviceName(names.get(dq.getDeviceId()));
            dto.setSensorCode(dq.getSensorCode());
            dto.setCategory(dq.getCategory());
            dto.setIssueType(dq.getIssueType());
            dto.setSeverity(dq.getSeverity());
            dto.setDetail(dq.getDetail());
            dto.setLatestValue(dq.getLatestValue());
            dto.setFirstSeen(dq.getFirstSeen());
            dto.setLastSeen(dq.getLastSeen());
            dto.setOccurrenceCount(dq.getOccurrenceCount());
            out.add(dto);
        }
        return Result.ok(out);
    }

    @GetMapping("/status")
    public Result<QualityStatusDTO> status(@RequestParam Integer deviceId) {
        List<DataQuality> rows = dataQualityService.queryIssues(deviceId, null, null, 200);
        List<QualityIssueDTO> dtos = new ArrayList<>();
        for (DataQuality dq : rows) {
            QualityIssueDTO dto = new QualityIssueDTO();
            dto.setId(dq.getId());
            dto.setDeviceId(dq.getDeviceId());
            dto.setSensorCode(dq.getSensorCode());
            dto.setCategory(dq.getCategory());
            dto.setIssueType(dq.getIssueType());
            dto.setSeverity(dq.getSeverity());
            dto.setDetail(dq.getDetail());
            dto.setLatestValue(dq.getLatestValue());
            dto.setFirstSeen(dq.getFirstSeen());
            dto.setLastSeen(dq.getLastSeen());
            dto.setOccurrenceCount(dq.getOccurrenceCount());
            dtos.add(dto);
        }
        return Result.ok(new QualityStatusDTO(deviceId, dataQualityService.resolveStatus(deviceId), dtos));
    }
}
