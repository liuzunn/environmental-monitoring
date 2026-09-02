package org.nep.nepsystem.dto;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

/**
 * 巡检检测记录 VO（Phase 6 新增）：管理员查看检测结果
 */
public class InspectionRecordVO {
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;
    @JsonSerialize(using = ToStringSerializer.class)
    private Long taskId;
    private String recordType;
    private String content;
    private BigDecimal pm25;
    private BigDecimal pm10;
    private BigDecimal so2;
    private BigDecimal no2;
    private BigDecimal co;
    private BigDecimal o3;
    private Integer aqiValue;
    private List<String> images;
    private BigDecimal lat;
    private BigDecimal lng;
    private Integer recorderId;
    private String recorderName;
    private Date createTime;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getTaskId() { return taskId; }
    public void setTaskId(Long taskId) { this.taskId = taskId; }
    public String getRecordType() { return recordType; }
    public void setRecordType(String recordType) { this.recordType = recordType; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public BigDecimal getPm25() { return pm25; }
    public void setPm25(BigDecimal pm25) { this.pm25 = pm25; }
    public BigDecimal getPm10() { return pm10; }
    public void setPm10(BigDecimal pm10) { this.pm10 = pm10; }
    public BigDecimal getSo2() { return so2; }
    public void setSo2(BigDecimal so2) { this.so2 = so2; }
    public BigDecimal getNo2() { return no2; }
    public void setNo2(BigDecimal no2) { this.no2 = no2; }
    public BigDecimal getCo() { return co; }
    public void setCo(BigDecimal co) { this.co = co; }
    public BigDecimal getO3() { return o3; }
    public void setO3(BigDecimal o3) { this.o3 = o3; }
    public Integer getAqiValue() { return aqiValue; }
    public void setAqiValue(Integer aqiValue) { this.aqiValue = aqiValue; }
    public List<String> getImages() { return images; }
    public void setImages(List<String> images) { this.images = images; }
    public BigDecimal getLat() { return lat; }
    public void setLat(BigDecimal lat) { this.lat = lat; }
    public BigDecimal getLng() { return lng; }
    public void setLng(BigDecimal lng) { this.lng = lng; }
    public Integer getRecorderId() { return recorderId; }
    public void setRecorderId(Integer recorderId) { this.recorderId = recorderId; }
    public String getRecorderName() { return recorderName; }
    public void setRecorderName(String recorderName) { this.recorderName = recorderName; }
    public Date getCreateTime() { return createTime; }
    public void setCreateTime(Date createTime) { this.createTime = createTime; }
}
