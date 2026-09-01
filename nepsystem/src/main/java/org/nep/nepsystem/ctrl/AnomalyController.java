package org.nep.nepsystem.ctrl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.nep.nepsystem.bean.DataQuality;
import org.nep.nepsystem.bean.Devices;
import org.nep.nepsystem.common.Result;
import org.nep.nepsystem.dao.DataQualityDao;
import org.nep.nepsystem.dao.DevicesDao;
import org.nep.nepsystem.dto.AnomalySummaryDTO;
import org.nep.nepsystem.dto.QualityIssueDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 统计异常检测接口（业务层升级新增）：
 * GET /api/anomalies            - 异常记录列表（ANOMALY 类别）
 * GET /api/anomalies/summary    - 按问题类型汇总
 */
@RestController
@RequestMapping("/api/anomalies")
public class AnomalyController {

    @Autowired
    private DataQualityDao dataQualityDao;

    @Autowired
    private DevicesDao devicesDao;

    @GetMapping
    public Result<List<QualityIssueDTO>> list(@RequestParam(required = false) Integer deviceId,
                                              @RequestParam(required = false) String sensorCode,
                                              @RequestParam(required = false) String issueType,
                                              @RequestParam(defaultValue = "50") int limit) {
        QueryWrapper<DataQuality> qw = new QueryWrapper<DataQuality>().eq("category", "ANOMALY");
        if (deviceId != null) qw.eq("device_id", deviceId);
        if (StringUtils.hasText(sensorCode)) qw.eq("sensor_code", sensorCode);
        if (StringUtils.hasText(issueType)) qw.eq("issue_type", issueType);
        qw.orderByDesc("last_seen").last("limit " + Math.min(Math.max(limit, 1), 200));
        List<DataQuality> rows = dataQualityDao.selectList(qw);

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

    @GetMapping("/summary")
    public Result<List<AnomalySummaryDTO>> summary(@RequestParam(required = false) Integer deviceId) {
        QueryWrapper<DataQuality> qw = new QueryWrapper<DataQuality>()
                .eq("category", "ANOMALY")
                .select("issue_type", "COUNT(*) AS cnt")
                .groupBy("issue_type")
                .orderByDesc("cnt");
        if (deviceId != null) qw.eq("device_id", deviceId);
        List<Map<String, Object>> rows = dataQualityDao.selectMaps(qw);
        List<AnomalySummaryDTO> out = new ArrayList<>();
        for (Map<String, Object> r : rows) {
            out.add(new AnomalySummaryDTO(String.valueOf(r.get("issue_type")), ((Number) r.get("cnt")).longValue()));
        }
        return Result.ok(out);
    }
}
