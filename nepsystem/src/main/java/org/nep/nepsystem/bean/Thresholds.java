package org.nep.nepsystem.bean;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;

import java.math.BigDecimal;
import java.util.Date;

/**
 * thresholds 表实体
 */
@TableName(value = "thresholds")
public class Thresholds {

    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    /** 设备ID, NULL=全局默认 */
    @TableField(value = "device_id")
    private Integer deviceId;

    /** 指标编码 */
    @TableField(value = "sensor_code")
    private String sensorCode;

    /** 预警下限 */
    @TableField(value = "warn_min")
    private java.math.BigDecimal warnMin;

    /** 预警上限 */
    @TableField(value = "warn_max")
    private java.math.BigDecimal warnMax;

    /** 报警下限 */
    @TableField(value = "alarm_min")
    private java.math.BigDecimal alarmMin;

    /** 报警上限 */
    @TableField(value = "alarm_max")
    private java.math.BigDecimal alarmMax;

    /** 是否启用: 1启用 0停用 */
    @TableField(value = "enabled")
    private Integer enabled;

    /** 更新时间 */
    @TableField(value = "update_time")
    private java.util.Date updateTime;


    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
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

    public java.math.BigDecimal getWarnMin() {
        return warnMin;
    }

    public void setWarnMin(java.math.BigDecimal warnMin) {
        this.warnMin = warnMin;
    }

    public java.math.BigDecimal getWarnMax() {
        return warnMax;
    }

    public void setWarnMax(java.math.BigDecimal warnMax) {
        this.warnMax = warnMax;
    }

    public java.math.BigDecimal getAlarmMin() {
        return alarmMin;
    }

    public void setAlarmMin(java.math.BigDecimal alarmMin) {
        this.alarmMin = alarmMin;
    }

    public java.math.BigDecimal getAlarmMax() {
        return alarmMax;
    }

    public void setAlarmMax(java.math.BigDecimal alarmMax) {
        this.alarmMax = alarmMax;
    }

    public Integer getEnabled() {
        return enabled;
    }

    public void setEnabled(Integer enabled) {
        this.enabled = enabled;
    }

    public java.util.Date getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(java.util.Date updateTime) {
        this.updateTime = updateTime;
    }

}