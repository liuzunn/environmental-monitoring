package org.nep.nepsystem.dto;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;

import java.util.Date;

/**
 * 巡检任务 VO（Phase 4 新增）：任务 + 关联名称
 */
public class TaskVO {
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;
    private String taskNo;
    @JsonSerialize(using = ToStringSerializer.class)
    private Long eventId;
    private String eventTitle;
    private Integer deviceId;
    private String deviceName;
    private Integer gridId;
    private String gridName;
    private Integer assigneeId;
    private String assigneeName;
    private String taskType;
    private String priority;
    private String status;
    private Date deadline;
    private String result;
    private Date createTime;
    private Date updateTime;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getTaskNo() { return taskNo; }
    public void setTaskNo(String taskNo) { this.taskNo = taskNo; }
    public Long getEventId() { return eventId; }
    public void setEventId(Long eventId) { this.eventId = eventId; }
    public String getEventTitle() { return eventTitle; }
    public void setEventTitle(String eventTitle) { this.eventTitle = eventTitle; }
    public Integer getDeviceId() { return deviceId; }
    public void setDeviceId(Integer deviceId) { this.deviceId = deviceId; }
    public String getDeviceName() { return deviceName; }
    public void setDeviceName(String deviceName) { this.deviceName = deviceName; }
    public Integer getGridId() { return gridId; }
    public void setGridId(Integer gridId) { this.gridId = gridId; }
    public String getGridName() { return gridName; }
    public void setGridName(String gridName) { this.gridName = gridName; }
    public Integer getAssigneeId() { return assigneeId; }
    public void setAssigneeId(Integer assigneeId) { this.assigneeId = assigneeId; }
    public String getAssigneeName() { return assigneeName; }
    public void setAssigneeName(String assigneeName) { this.assigneeName = assigneeName; }
    public String getTaskType() { return taskType; }
    public void setTaskType(String taskType) { this.taskType = taskType; }
    public String getPriority() { return priority; }
    public void setPriority(String priority) { this.priority = priority; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Date getDeadline() { return deadline; }
    public void setDeadline(Date deadline) { this.deadline = deadline; }
    public String getResult() { return result; }
    public void setResult(String result) { this.result = result; }
    public Date getCreateTime() { return createTime; }
    public void setCreateTime(Date createTime) { this.createTime = createTime; }
    public Date getUpdateTime() { return updateTime; }
    public void setUpdateTime(Date updateTime) { this.updateTime = updateTime; }
}
