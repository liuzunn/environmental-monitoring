package org.nep.nepsystem.dto;

/**
 * NEPM 监管工作台统计（Phase 4 新增）
 */
public class WorkbenchStatsDTO {
    private Long todayEvents;
    private Long pendingReview;
    private Long pendingAssign;
    private Long processing;
    private Long todayClosed;

    public Long getTodayEvents() { return todayEvents; }
    public void setTodayEvents(Long todayEvents) { this.todayEvents = todayEvents; }
    public Long getPendingReview() { return pendingReview; }
    public void setPendingReview(Long pendingReview) { this.pendingReview = pendingReview; }
    public Long getPendingAssign() { return pendingAssign; }
    public void setPendingAssign(Long pendingAssign) { this.pendingAssign = pendingAssign; }
    public Long getProcessing() { return processing; }
    public void setProcessing(Long processing) { this.processing = processing; }
    public Long getTodayClosed() { return todayClosed; }
    public void setTodayClosed(Long todayClosed) { this.todayClosed = todayClosed; }
}
