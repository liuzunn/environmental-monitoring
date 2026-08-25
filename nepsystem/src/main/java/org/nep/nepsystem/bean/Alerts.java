package org.nep.nepsystem.bean;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;

import java.math.BigDecimal;
import java.util.Date;

/**
 * alerts 表实体
 */
@TableName(value = "alerts")
public class Alerts {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 设备ID */
    @TableField(value = "device_id")
    private Integer deviceId;

    /** 指标编码 */
    @TableField(value = "sensor_code")
    private String sensorCode;

    /** 级别: WARN预警/ALARM报警 */
    @TableField(value = "level")
    private String level;

    /** 触发告警的数值 */
    @TableField(value = "alert_value")
    private java.math.BigDecimal alertValue;

    /** 告警描述 */
    @TableField(value = "message")
    private String message;

    /** 状态: 0未处理 1已处理 */
    @TableField(value = "status")
    private Integer status;

    /** 处理人 */
    @TableField(value = "handle_user")
    private String handleUser;

    /** 处理时间 */
    @TableField(value = "handle_time")
    private java.util.Date handleTime;

    /** 告警时间 */
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

    public String getLevel() {
        return level;
    }

    public void setLevel(String level) {
        this.level = level;
    }

    public java.math.BigDecimal getAlertValue() {
        return alertValue;
    }

    public void setAlertValue(java.math.BigDecimal alertValue) {
        this.alertValue = alertValue;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public String getHandleUser() {
        return handleUser;
    }

    public void setHandleUser(String handleUser) {
        this.handleUser = handleUser;
    }

    public java.util.Date getHandleTime() {
        return handleTime;
    }

    public void setHandleTime(java.util.Date handleTime) {
        this.handleTime = handleTime;
    }

    public java.util.Date getCreateTime() {
        return createTime;
    }

    public void setCreateTime(java.util.Date createTime) {
        this.createTime = createTime;
    }

}