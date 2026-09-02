package org.nep.nepsystem.dto;

/**
 * 网格员任务统计（Phase 5 新增）：首页统计卡
 */
public class TaskStatsVO {
    private Long pendingAccept;
    private Long processing;
    private Long todayDone;
    private Long overdue;

    public Long getPendingAccept() { return pendingAccept; }
    public void setPendingAccept(Long pendingAccept) { this.pendingAccept = pendingAccept; }
    public Long getProcessing() { return processing; }
    public void setProcessing(Long processing) { this.processing = processing; }
    public Long getTodayDone() { return todayDone; }
    public void setTodayDone(Long todayDone) { this.todayDone = todayDone; }
    public Long getOverdue() { return overdue; }
    public void setOverdue(Long overdue) { this.overdue = overdue; }
}
