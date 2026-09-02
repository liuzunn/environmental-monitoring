package org.nep.nepsystem.bean;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.math.BigDecimal;
import java.util.Date;

/**
 * inspection_record 表实体
 */
@TableName(value = "inspection_record")
public class InspectionRecord {

    /** 记录ID(主键) */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 巡检任务ID */
    @TableField(value = "task_id")
    private Long taskId;

    /** 记录类型: INSPECT巡检/VERIFY核实 */
    @TableField(value = "record_type")
    private String recordType;

    /** 记录内容 */
    @TableField(value = "content")
    private String content;

    /** PM2.5检测值(ug/m3)（v6 新增） */
    @TableField(value = "pm25")
    private java.math.BigDecimal pm25;

    /** PM10检测值(ug/m3)（v6 新增） */
    @TableField(value = "pm10")
    private java.math.BigDecimal pm10;

    /** SO2检测值(ug/m3)（v6 新增） */
    @TableField(value = "so2")
    private java.math.BigDecimal so2;

    /** NO2检测值(ug/m3)（v6 新增） */
    @TableField(value = "no2")
    private java.math.BigDecimal no2;

    /** CO检测值(mg/m3)（v6 新增） */
    @TableField(value = "co")
    private java.math.BigDecimal co;

    /** O3检测值(ug/m3)（v6 新增） */
    @TableField(value = "o3")
    private java.math.BigDecimal o3;

    /** 计算AQI(有值项IAQI最大值)（v6 新增） */
    @TableField(value = "aqi_value")
    private Integer aqiValue;

    /** 检测纬度(可空, WGS84)（v6 新增） */
    @TableField(value = "lat")
    private java.math.BigDecimal lat;

    /** 检测经度(可空, WGS84)（v6 新增） */
    @TableField(value = "lng")
    private java.math.BigDecimal lng;

    /** 现场图片路径(逗号分隔) */
    @TableField(value = "images")
    private String images;

    /** 记录人ID(可空) */
    @TableField(value = "recorder_id")
    private Integer recorderId;

    /** 记录时间 */
    @TableField(value = "create_time")
    private Date createTime;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getTaskId() {
        return taskId;
    }

    public void setTaskId(Long taskId) {
        this.taskId = taskId;
    }

    public String getRecordType() {
        return recordType;
    }

    public void setRecordType(String recordType) {
        this.recordType = recordType;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public java.math.BigDecimal getPm25() { return pm25; }
    public void setPm25(java.math.BigDecimal pm25) { this.pm25 = pm25; }
    public java.math.BigDecimal getPm10() { return pm10; }
    public void setPm10(java.math.BigDecimal pm10) { this.pm10 = pm10; }
    public java.math.BigDecimal getSo2() { return so2; }
    public void setSo2(java.math.BigDecimal so2) { this.so2 = so2; }
    public java.math.BigDecimal getNo2() { return no2; }
    public void setNo2(java.math.BigDecimal no2) { this.no2 = no2; }
    public java.math.BigDecimal getCo() { return co; }
    public void setCo(java.math.BigDecimal co) { this.co = co; }
    public java.math.BigDecimal getO3() { return o3; }
    public void setO3(java.math.BigDecimal o3) { this.o3 = o3; }
    public Integer getAqiValue() { return aqiValue; }
    public void setAqiValue(Integer aqiValue) { this.aqiValue = aqiValue; }
    public java.math.BigDecimal getLat() { return lat; }
    public void setLat(java.math.BigDecimal lat) { this.lat = lat; }
    public java.math.BigDecimal getLng() { return lng; }
    public void setLng(java.math.BigDecimal lng) { this.lng = lng; }

    public String getImages() {
        return images;
    }

    public void setImages(String images) {
        this.images = images;
    }

    public Integer getRecorderId() {
        return recorderId;
    }

    public void setRecorderId(Integer recorderId) {
        this.recorderId = recorderId;
    }

    public Date getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }
}