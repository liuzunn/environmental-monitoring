package org.nep.nepsystem.dto;

import java.math.BigDecimal;
import java.util.List;

/**
 * 创建监督事件请求体（Phase 2 新增）：
 * 提交人身份由请求头 X-User-Id 提供，body 不允许携带 userId（防止伪造他人提交）。
 * status 不允许由前端传入，状态转换由 Service 控制。
 */
public class SupervisionCreateDTO {
    private String eventType;
    private String title;
    private String description;
    private Integer deviceId;
    private Integer regionId;
    private String location;
    private BigDecimal lat;
    private BigDecimal lng;
    private String level;
    /** 附件登记列表（可选，文件实体存储由后续 Phase 提供） */
    private List<SupervisionAttachmentDTO> attachments;

    public String getEventType() { return eventType; }
    public void setEventType(String eventType) { this.eventType = eventType; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public Integer getDeviceId() { return deviceId; }
    public void setDeviceId(Integer deviceId) { this.deviceId = deviceId; }
    public Integer getRegionId() { return regionId; }
    public void setRegionId(Integer regionId) { this.regionId = regionId; }
    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }
    public BigDecimal getLat() { return lat; }
    public void setLat(BigDecimal lat) { this.lat = lat; }
    public BigDecimal getLng() { return lng; }
    public void setLng(BigDecimal lng) { this.lng = lng; }
    public String getLevel() { return level; }
    public void setLevel(String level) { this.level = level; }
    public List<SupervisionAttachmentDTO> getAttachments() { return attachments; }
    public void setAttachments(List<SupervisionAttachmentDTO> attachments) { this.attachments = attachments; }
}
