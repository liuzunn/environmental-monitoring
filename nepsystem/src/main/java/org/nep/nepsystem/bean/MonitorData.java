package org.nep.nepsystem.bean;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;

import java.math.BigDecimal;
import java.util.Date;

/**
 * monitor_data 表实体
 */
@TableName(value = "monitor_data")
public class MonitorData {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 设备ID */
    @TableField(value = "device_id")
    private Integer deviceId;

    /** 指标编码 */
    @TableField(value = "sensor_code")
    private String sensorCode;

    /** 监测数值 */
    @TableField(value = "value")
    private java.math.BigDecimal value;

    /** 上报时间 */
    @TableField(value = "report_time")
    private java.util.Date reportTime;

    /** 入库时间 */
    @TableField(value = "create_time")
    private java.util.Date createTime;


    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Integer getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(Integer deviceId) {
        this.deviceId = deviceId;
    }

    public String getSensorCode() {
        return sensorCode;
    }

    public void setSensorCode(String sensorCode) {
        this.sensorCode = sensorCode;
    }

    public java.math.BigDecimal getValue() {
        return value;
    }

    public void setValue(java.math.BigDecimal value) {
        this.value = value;
    }

    public java.util.Date getReportTime() {
        return reportTime;
    }

    public void setReportTime(java.util.Date reportTime) {
        this.reportTime = reportTime;
    }

    public java.util.Date getCreateTime() {
        return createTime;
    }

    public void setCreateTime(java.util.Date createTime) {
        this.createTime = createTime;
    }

}