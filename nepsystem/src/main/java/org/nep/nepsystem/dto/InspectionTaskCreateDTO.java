package org.nep.nepsystem.dto;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.util.Date;

/**
 * 创建巡检任务请求体（Phase 4 新增，管理员）：
 * {gridId, deviceId, assigneeId, eventId, taskType, priority, deadline, description}
 */
public class InspectionTaskCreateDTO {
    private Integer gridId;
    private Integer deviceId;
    private Integer assigneeId;
    private Long eventId;
    private String taskType;
    private String priority;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date deadline;
    private String description;

    public Integer getGridId() { return gridId; }
    public void setGridId(Integer gridId) { this.gridId = gridId; }
    public Integer getDeviceId() { return deviceId; }
    public void setDeviceId(Integer deviceId) { this.deviceId = deviceId; }
    public Integer getAssigneeId() { return assigneeId; }
    public void setAssigneeId(Integer assigneeId) { this.assigneeId = assigneeId; }
    public Long getEventId() { return eventId; }
    public void setEventId(Long eventId) { this.eventId = eventId; }
    public String getTaskType() { return taskType; }
    public void setTaskType(String taskType) { this.taskType = taskType; }
    public String getPriority() { return priority; }
    public void setPriority(String priority) { this.priority = priority; }
    public Date getDeadline() { return deadline; }
    public void setDeadline(Date deadline) { this.deadline = deadline; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
}
