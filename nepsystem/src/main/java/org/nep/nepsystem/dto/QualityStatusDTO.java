package org.nep.nepsystem.dto;

import java.util.List;

/** 设备数据质量状态 DTO（业务层升级新增） */
public class QualityStatusDTO {
    private Integer deviceId;
    private String status; // GOOD / WARNING / BAD
    private List<QualityIssueDTO> issues;

    public QualityStatusDTO() {
    }

    public QualityStatusDTO(Integer deviceId, String status, List<QualityIssueDTO> issues) {
        this.deviceId = deviceId;
        this.status = status;
        this.issues = issues;
    }

    public Integer getDeviceId() { return deviceId; }
    public void setDeviceId(Integer deviceId) { this.deviceId = deviceId; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public List<QualityIssueDTO> getIssues() { return issues; }
    public void setIssues(List<QualityIssueDTO> issues) { this.issues = issues; }
}
