package org.nep.nepsystem.bean;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;

import java.math.BigDecimal;
import java.util.Date;

/**
 * devices 表实体
 */
@TableName(value = "devices")
public class Devices {

    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    /** 设备编号(唯一) */
    @TableField(value = "device_code")
    private String deviceCode;

    /** 设备名称 */
    @TableField(value = "device_name")
    private String deviceName;

    /** 设备类型: AIR/WATER/NOISE */
    @TableField(value = "type")
    private String type;

    /** 所属区域ID */
    @TableField(value = "region_id")
    private Integer regionId;

    /** 安装位置 */
    @TableField(value = "location")
    private String location;

    /** 纬度（空间态势升级新增，可空，WGS84） */
    @TableField(value = "lat")
    private java.math.BigDecimal lat;

    /** 经度（空间态势升级新增，可空，WGS84） */
    @TableField(value = "lng")
    private java.math.BigDecimal lng;

    /** 状态: 0离线 1在线 2停用 */
    @TableField(value = "status")
    private Integer status;

    /** 最近上报时间 */
    @TableField(value = "last_report_time")
    private java.util.Date lastReportTime;

    /** 创建时间 */
    @TableField(value = "create_time")
    private java.util.Date createTime;


    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getDeviceCode() {
        return deviceCode;
    }

    public void setDeviceCode(String deviceCode) {
        this.deviceCode = deviceCode;
    }

    public String getDeviceName() {
        return deviceName;
    }

    public void setDeviceName(String deviceName) {
        this.deviceName = deviceName;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Integer getRegionId() {
        return regionId;
    }

    public void setRegionId(Integer regionId) {
        this.regionId = regionId;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public java.util.Date getLastReportTime() {
        return lastReportTime;
    }

    public void setLastReportTime(java.util.Date lastReportTime) {
        this.lastReportTime = lastReportTime;
    }

    public java.util.Date getCreateTime() {
        return createTime;
    }

    public void setCreateTime(java.util.Date createTime) {
        this.createTime = createTime;
    }


    public java.math.BigDecimal getLat() {
        return lat;
    }

    public void setLat(java.math.BigDecimal lat) {
        this.lat = lat;
    }

    public java.math.BigDecimal getLng() {
        return lng;
    }

    public void setLng(java.math.BigDecimal lng) {
        this.lng = lng;
    }
}