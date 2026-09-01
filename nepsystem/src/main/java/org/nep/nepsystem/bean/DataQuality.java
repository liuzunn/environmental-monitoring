package org.nep.nepsystem.bean;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.math.BigDecimal;
import java.util.Date;

/**
 * data_quality 表实体：数据质量与统计异常检测记录（快照式，每设备每指标每问题类型一行）
 * 业务层升级新增
 */
@TableName(value = "data_quality")
public class DataQuality {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 设备ID */
    @TableField(value = "device_id")
    private Integer deviceId;

    /** 指标编码（NULL=设备级问题，如离线） */
    @TableField(value = "sensor_code")
    private String sensorCode;

    /** 类别: QUALITY数据质量 / ANOMALY统计异常 */
    @TableField(value = "category")
    private String category;

    /** 问题类型: NULL_VALUE/OUT_OF_RANGE/CONSTANT_VALUE/INTERVAL_ABNORMAL/DEVICE_OFFLINE/ZSCORE/CONSECUTIVE_EXCEED/SUDDEN_CHANGE */
    @TableField(value = "issue_type")
    private String issueType;

    /** 级别: INFO/WARN/CRITICAL */
    @TableField(value = "severity")
    private String severity;

    /** 描述 */
    @TableField(value = "detail")
    private String detail;

    /** 最近一次触发值 */
    @TableField(value = "latest_value")
    private BigDecimal latestValue;

    /** 首次发现时间 */
    @TableField(value = "first_seen")
    private Date firstSeen;

    /** 最近发现时间 */
    @TableField(value = "last_seen")
    private Date lastSeen;

    /** 累计发生次数 */
    @TableField(value = "occurrence_count")
    private Integer occurrenceCount;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Integer getDeviceId() { return deviceId; }
    public void setDeviceId(Integer deviceId) { this.deviceId = deviceId; }
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
