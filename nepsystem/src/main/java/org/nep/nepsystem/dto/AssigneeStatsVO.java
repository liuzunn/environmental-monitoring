package org.nep.nepsystem.dto;

/**
 * 网格员任务完成率统计 VO（Phase 4 新增）
 */
public class AssigneeStatsVO {
    private Integer userId;
    private String userName;
    private Long totalTasks;
    private Long closedTasks;
    private Integer completionRate;

    public Integer getUserId() { return userId; }
    public void setUserId(Integer userId) { this.userId = userId; }
    public String getUserName() { return userName; }
    public void setUserName(String userName) { this.userName = userName; }
    public Long getTotalTasks() { return totalTasks; }
    public void setTotalTasks(Long totalTasks) { this.totalTasks = totalTasks; }
    public Long getClosedTasks() { return closedTasks; }
    public void setClosedTasks(Long closedTasks) { this.closedTasks = closedTasks; }
    public Integer getCompletionRate() { return completionRate; }
    public void setCompletionRate(Integer completionRate) { this.completionRate = completionRate; }
}
