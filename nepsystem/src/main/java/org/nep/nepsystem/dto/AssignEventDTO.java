package org.nep.nepsystem.dto;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.util.Date;

/**
 * 监督事件派单请求体（Phase 4 新增）：
 * PUT /api/supervision/{id}/assign  body: {gridId, assigneeId, priority, deadline, remark}
 */
public class AssignEventDTO {
    private Integer gridId;
    private Integer assigneeId;
    private String priority;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date deadline;
    private String remark;

    public Integer getGridId() { return gridId; }
    public void setGridId(Integer gridId) { this.gridId = gridId; }
    public Integer getAssigneeId() { return assigneeId; }
    public void setAssigneeId(Integer assigneeId) { this.assigneeId = assigneeId; }
    public String getPriority() { return priority; }
    public void setPriority(String priority) { this.priority = priority; }
    public Date getDeadline() { return deadline; }
    public void setDeadline(Date deadline) { this.deadline = deadline; }
    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }
}
