package org.nep.nepsystem.dto;

import java.util.Date;

/**
 * 设备健康度 DTO（业务层升级新增）：
 * 在线率 / 最近通信时间 / 数据完整率 / 异常次数 / 告警次数 -> healthScore(0-100)
 * healthLevel: HEALTHY(>=80) / FAIR(>=60) / POOR(<60)
 */
public class DeviceHealthDTO {
    private Integer deviceId;
    private String deviceCode;
    private String deviceName;
    private String type;
    private Integer status;

    /** 健康分 0-100 */
    private Double healthScore;
    /** HEALTHY / FAIR / POOR */
    private String healthLevel;

    /** 在线率(%)：近 7 天有数据的天数 / 7 */
    private Double onlineRate;
    /** 最近通信时间 */
    private Date lastCommunication;
    /** 数据完整率(%)：近 24h 上报指标数 / 该类型应上报指标数 */
    private Double dataCompleteness;
    /** 异常次数：近 7 天仍活跃的统计异常累计触发次数 */
    private Long anomalyCount;
    /** 告警次数：近 7 天告警数 */
    private Long alertCount;

    public Integer getDeviceId() { return deviceId; }
    public void setDeviceId(Integer deviceId) { this.deviceId = deviceId; }
    public String getDeviceCode() { return deviceCode; }
    public void setDeviceCode(String deviceCode) { this.deviceCode = deviceCode; }
    public String getDeviceName() { return deviceName; }
    public void setDeviceName(String deviceName) { this.deviceName = deviceName; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }
    public Double getHealthScore() { return healthScore; }
    public void setHealthScore(Double healthScore) { this.healthScore = healthScore; }
    public String getHealthLevel() { return healthLevel; }
    public void setHealthLevel(String healthLevel) { this.healthLevel = healthLevel; }
    public Double getOnlineRate() { return onlineRate; }
    public void setOnlineRate(Double onlineRate) { this.onlineRate = onlineRate; }
    public Date getLastCommunication() { return lastCommunication; }
    public void setLastCommunication(Date lastCommunication) { this.lastCommunication = lastCommunication; }
    public Double getDataCompleteness() { return dataCompleteness; }
    public void setDataCompleteness(Double dataCompleteness) { this.dataCompleteness = dataCompleteness; }
    public Long getAnomalyCount() { return anomalyCount; }
    public void setAnomalyCount(Long anomalyCount) { this.anomalyCount = anomalyCount; }
    public Long getAlertCount() { return alertCount; }
    public void setAlertCount(Long alertCount) { this.alertCount = alertCount; }
}
