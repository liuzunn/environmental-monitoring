package org.nep.nepsystem.dto;

import java.math.BigDecimal;
import java.util.Date;

/** 数据质量/异常检测记录 DTO（业务层升级新增） */
public class QualityIssueDTO {
    private Long id;
    private Integer deviceId;
    private String deviceName;
    private String sensorCode;
    private String category;
    private String issueType;
    private String severity;
    private String detail;
    private BigDecimal latestValue;
    private Date firstSeen;
    private Date lastSeen;
    private Integer occurrenceCount;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Integer getDeviceId() { return deviceId; }
    public void setDeviceId(Integer deviceId) { this.deviceId = deviceId; }
    public String getDeviceName() { return deviceName; }
    public void setDeviceName(String deviceName) { this.deviceName = deviceName; }
    public String getSensorCode() { return sensorCode; }
    public void setSensorCode(String sensorCode) { this.sensorCode = sensorCode; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public String getIssueType() { return issueType; }
    public void setIssueType(String issueType) { this.issueType = issueType; }
    public String getSeverity() { return severity; }
    public void setSeverity(String severity) { this.severity = severity; }
    public String getDetail() { return detail; }
    public void setDetail(String detail) { this.detail = detail; }
    public BigDecimal getLatestValue() { return latestValue; }
    public void setLatestValue(BigDecimal latestValue) { this.latestValue = latestValue; }
    public Date getFirstSeen() { return firstSeen; }
    public void setFirstSeen(Date firstSeen) { this.firstSeen = firstSeen; }
    public Date getLastSeen() { return lastSeen; }
    public void setLastSeen(Date lastSeen) { this.lastSeen = lastSeen; }
    public Integer getOccurrenceCount() { return occurrenceCount; }
    public void setOccurrenceCount(Integer occurrenceCount) { this.occurrenceCount = occurrenceCount; }
}
