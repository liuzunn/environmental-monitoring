package org.nep.nepsystem.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * 设备空间态势 DTO（空间态势升级新增）：
 * 状态分类：在线/离线/预警(WARN 活跃告警)/报警(ALARM 活跃告警)；
 * 视觉信息：正常/超标(任一指标超标准限值)/离线/选中；
 * 详情：名称/编号/类型/位置/当前数据/在线状态/健康度/当前告警。
 */
public class DeviceSituationDTO {
    private Integer deviceId;
    private String deviceCode;
    private String deviceName;
    private String type;
    private String location;
    private BigDecimal lat;
    private BigDecimal lng;
    private Integer status;
    private Boolean online;
    private Double healthScore;
    private String healthLevel;
    private String qualityStatus;
    private Boolean warn;
    private Boolean alarm;
    /** 当前数据: sensorCode -> {value, reportTime} */
    private Map<String, Map<String, Object>> values;
    /** 当前活跃告警（status=0 且未结束） */
    private List<Map<String, Object>> alerts;

    public Integer getDeviceId() { return deviceId; }
    public void setDeviceId(Integer deviceId) { this.deviceId = deviceId; }
    public String getDeviceCode() { return deviceCode; }
    public void setDeviceCode(String deviceCode) { this.deviceCode = deviceCode; }
    public String getDeviceName() { return deviceName; }
    public void setDeviceName(String deviceName) { this.deviceName = deviceName; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }
    public BigDecimal getLat() { return lat; }
    public void setLat(BigDecimal lat) { this.lat = lat; }
    public BigDecimal getLng() { return lng; }
    public void setLng(BigDecimal lng) { this.lng = lng; }
    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }
    public Boolean getOnline() { return online; }
    public void setOnline(Boolean online) { this.online = online; }
    public Double getHealthScore() { return healthScore; }
    public void setHealthScore(Double healthScore) { this.healthScore = healthScore; }
    public String getHealthLevel() { return healthLevel; }
    public void setHealthLevel(String healthLevel) { this.healthLevel = healthLevel; }
    public String getQualityStatus() { return qualityStatus; }
    public void setQualityStatus(String qualityStatus) { this.qualityStatus = qualityStatus; }
    public Boolean getWarn() { return warn; }
    public void setWarn(Boolean warn) { this.warn = warn; }
    public Boolean getAlarm() { return alarm; }
    public void setAlarm(Boolean alarm) { this.alarm = alarm; }
    public Map<String, Map<String, Object>> getValues() { return values; }
    public void setValues(Map<String, Map<String, Object>> values) { this.values = values; }
    public List<Map<String, Object>> getAlerts() { return alerts; }
    public void setAlerts(List<Map<String, Object>> alerts) { this.alerts = alerts; }
}
