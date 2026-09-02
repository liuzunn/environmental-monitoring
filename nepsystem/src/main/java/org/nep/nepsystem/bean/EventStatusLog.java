package org.nep.nepsystem.bean;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.math.BigDecimal;
import java.util.Date;

/**
 * event_status_log 表实体
 */
@TableName(value = "event_status_log")
public class EventStatusLog {

    /** 日志ID(主键) */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 监督事件ID */
    @TableField(value = "event_id")
    @com.fasterxml.jackson.databind.annotation.JsonSerialize(using = com.fasterxml.jackson.databind.ser.std.ToStringSerializer.class)
    private Long eventId;

    /** 原状态(创建时为NULL) */
    @TableField(value = "from_status")
    private String fromStatus;

    /** 新状态 */
    @TableField(value = "to_status")
    private String toStatus;

    /** 操作人ID(可空=系统) */
    @TableField(value = "operator_id")
    private Integer operatorId;

    /** 备注 */
    @TableField(value = "remark")
    private String remark;

    /** 操作时间 */
    @TableField(value = "create_time")
    private Date createTime;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getEventId() {
        return eventId;
    }

    public void setEventId(Long eventId) {
        this.eventId = eventId;
    }

    public String getFromStatus() {
        return fromStatus;
    }

    public void setFromStatus(String fromStatus) {
        this.fromStatus = fromStatus;
    }

    public String getToStatus() {
        return toStatus;
    }

    public void setToStatus(String toStatus) {
        this.toStatus = toStatus;
    }

    public Integer getOperatorId() {
        return operatorId;
    }

    public void setOperatorId(Integer operatorId) {
        this.operatorId = operatorId;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }

    public Date getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }
}
