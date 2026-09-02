package org.nep.nepsystem.dto;

/** 空间态势总览 DTO（空间态势升级新增） */
public class SituationOverviewDTO {
    private Long totalDevices;
    private Long onlineDevices;
    private Long offlineDevices;
    private Long warnDevices;
    private Long alarmDevices;
    private Long activeAlerts;
    /** 环境状态良好 = 无活跃告警 */
    private Boolean healthy;

    public Long getTotalDevices() { return totalDevices; }
    public void setTotalDevices(Long totalDevices) { this.totalDevices = totalDevices; }
    public Long getOnlineDevices() { return onlineDevices; }
    public void setOnlineDevices(Long onlineDevices) { this.onlineDevices = onlineDevices; }
    public Long getOfflineDevices() { return offlineDevices; }
    public void setOfflineDevices(Long offlineDevices) { this.offlineDevices = offlineDevices; }
    public Long getWarnDevices() { return warnDevices; }
    public void setWarnDevices(Long warnDevices) { this.warnDevices = warnDevices; }
    public Long getAlarmDevices() { return alarmDevices; }
    public void setAlarmDevices(Long alarmDevices) { this.alarmDevices = alarmDevices; }
    public Long getActiveAlerts() { return activeAlerts; }
    public void setActiveAlerts(Long activeAlerts) { this.activeAlerts = activeAlerts; }
    public Boolean getHealthy() { return healthy; }
    public void setHealthy(Boolean healthy) { this.healthy = healthy; }
}
