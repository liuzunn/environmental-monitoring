package org.nep.nepsystem.bean;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.math.BigDecimal;
import java.util.Date;

/**
 * inspection_task 表实体
 */
@TableName(value = "inspection_task")
public class InspectionTask {

    /** 任务ID(主键) */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 任务编号(唯一) */
    @TableField(value = "task_no")
    private String taskNo;

    /** 关联监督事件ID(可空=独立巡检) */
    @TableField(value = "event_id")
    private Long eventId;

    /** 巡检设备ID(可空) */
    @TableField(value = "device_id")
    private Integer deviceId;

    /** 所属网格ID(可空) */
    @TableField(value = "grid_id")
    private Integer gridId;

    /** 执行网格员ID(可空) */
    @TableField(value = "assignee_id")
    private Integer assigneeId;

    /** 任务类型: INSPECTION巡检/VERIFY核实 */
    @TableField(value = "task_type")
    private String taskType;

    /** 优先级: LOW/MEDIUM/HIGH（v5 新增） */
    @TableField(value = "priority")
    private String priority;

    /** 状态机: PENDING_REVIEW/APPROVED/REJECTED/ASSIGNED/ACCEPTED/INSPECTING/INSPECTED/VERIFIED/CLOSED */
    @TableField(value = "status")
    private String status;

    /** 截止时间 */
    @TableField(value = "deadline")
    private Date deadline;

    /** 巡检结论 */
    @TableField(value = "result")
    private String result;

    /** 创建时间 */
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

    public String getTaskNo() {
        return taskNo;
    }

    public void setTaskNo(String taskNo) {
        this.taskNo = taskNo;
    }

    public Long getEventId() {
        return eventId;
    }

    public void setEventId(Long eventId) {
        this.eventId = eventId;
    }

    public Integer getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(Integer deviceId) {
        this.deviceId = deviceId;
    }

    public Integer getGridId() {
        return gridId;
    }

    public void setGridId(Integer gridId) {
        this.gridId = gridId;
    }

    public Integer getAssigneeId() {
        return assigneeId;
    }

    public void setAssigneeId(Integer assigneeId) {
        this.assigneeId = assigneeId;
    }

    public String getTaskType() {
        return taskType;
    }

    public void setTaskType(String taskType) {
        this.taskType = taskType;
    }

    public String getPriority() {
        return priority;
    }

    public void setPriority(String priority) {
        this.priority = priority;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Date getDeadline() {
        return deadline;
    }

    public void setDeadline(Date deadline) {
        this.deadline = deadline;
    }

    public String getResult() {
        return result;
    }

    public void setResult(String result) {
        this.result = result;
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
