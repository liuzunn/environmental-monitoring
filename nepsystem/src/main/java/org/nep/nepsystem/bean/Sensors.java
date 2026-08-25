package org.nep.nepsystem.bean;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;

import java.math.BigDecimal;
import java.util.Date;

/**
 * sensors 表实体
 */
@TableName(value = "sensors")
public class Sensors {

    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    /** 指标编码(唯一) */
    @TableField(value = "sensor_code")
    private String sensorCode;

    /** 指标名称 */
    @TableField(value = "sensor_name")
    private String sensorName;

    /** 单位 */
    @TableField(value = "unit")
    private String unit;

    /** 适用设备类型 */
    @TableField(value = "device_type")
    private String deviceType;

    /** 量程下限 */
    @TableField(value = "min_range")
    private java.math.BigDecimal minRange;

    /** 量程上限 */
    @TableField(value = "max_range")
    private java.math.BigDecimal maxRange;

    /** 标准限值 */
    @TableField(value = "standard_max")
    private java.math.BigDecimal standardMax;


    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getSensorCode() {
        return sensorCode;
    }

    public void setSensorCode(String sensorCode) {
        this.sensorCode = sensorCode;
    }

    public String getSensorName() {
        return sensorName;
    }

    public void setSensorName(String sensorName) {
        this.sensorName = sensorName;
    }

    public String getUnit() {
        return unit;
    }

    public void setUnit(String unit) {
        this.unit = unit;
    }

    public String getDeviceType() {
        return deviceType;
    }

    public void setDeviceType(String deviceType) {
        this.deviceType = deviceType;
    }

    public java.math.BigDecimal getMinRange() {
        return minRange;
    }

    public void setMinRange(java.math.BigDecimal minRange) {
        this.minRange = minRange;
    }

    public java.math.BigDecimal getMaxRange() {
        return maxRange;
    }

    public void setMaxRange(java.math.BigDecimal maxRange) {
        this.maxRange = maxRange;
    }

    public java.math.BigDecimal getStandardMax() {
        return standardMax;
    }

    public void setStandardMax(java.math.BigDecimal standardMax) {
        this.standardMax = standardMax;
    }

}