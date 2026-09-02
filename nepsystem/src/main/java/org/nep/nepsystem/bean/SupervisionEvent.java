package org.nep.nepsystem.bean;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.math.BigDecimal;
import java.util.Date;

/**
 * supervision_event 表实体
 */
@TableName(value = "supervision_event")
public class SupervisionEvent {

    /** 事件ID(主键) */
    @TableId(value = "id", type = IdType.AUTO)
    @com.fasterxml.jackson.databind.annotation.JsonSerialize(using = com.fasterxml.jackson.databind.ser.std.ToStringSerializer.class)
    private Long id;

    /** 事件编号(唯一) */
    @TableField(value = "event_no")
    private String eventNo;

    /** 提交人ID, NULL=匿名 */
    @TableField(value = "user_id")
    private Integer userId;

    /** 事件类型: POLLUTION/NOISE/DEVICE_FAULT/OTHER */
    @TableField(value = "event_type")
    private String eventType;

    /** 事件标题 */
    @TableField(value = "title")
    private String title;

    /** 事件描述 */
    @TableField(value = "description")
    private String description;

    /** 关联设备ID(可空) */
    @TableField(value = "device_id")
    private Integer deviceId;

    /** 关联区域ID(可空) */
    @TableField(value = "region_id")
    private Integer regionId;

    /** 事发位置描述 */
    @TableField(value = "location")
    private String location;

    /** 纬度(可空, WGS84) */
    @TableField(value = "lat")
    private BigDecimal lat;

    /** 经度(可空, WGS84) */
    @TableField(value = "lng")
    private BigDecimal lng;

    /** 严重程度: WARN/ALARM */
    @TableField(value = "level")
    private String level;

    /** 状态机: PENDING_REVIEW/APPROVED/REJECTED/ASSIGNED/ACCEPTED/INSPECTING/INSPECTED/VERIFIED/CLOSED */
    @TableField(value = "status")
    private String status;

    /** 当前处理人ID(网格员) */
    @TableField(value = "assignee_id")
    private Integer assigneeId;

    /** 提交时间 */
    @TableField(value = "create_time")
    private Date createTime;

    /** 更新时间 */
    @TableField(value = "update_time")
    private Date updateTime;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getEventNo() {
        return eventNo;
    }

    public void setEventNo(String eventNo) {
        this.eventNo = eventNo;
    }

    public Integer getUserId() {
        return userId;
    }

    public void setUserId(Integer userId) {
        this.userId = userId;
    }

    public String getEventType() {
        return eventType;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Integer getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(Integer deviceId) {
        this.deviceId = deviceId;
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

    public BigDecimal getLat() {
        return lat;
    }

    public void setLat(BigDecimal lat) {
        this.lat = lat;
    }

    public BigDecimal getLng() {
        return lng;
    }

    public void setLng(BigDecimal lng) {
        this.lng = lng;
    }

    public String getLevel() {
        return level;
    }

    public void setLevel(String level) {
        this.level = level;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Integer getAssigneeId() {
        return assigneeId;
    }

    public void setAssigneeId(Integer assigneeId) {
        this.assigneeId = assigneeId;
    }

    public Date getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }

    public Date getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(Date updateTime) {
        this.updateTime = updateTime;
    }
}
