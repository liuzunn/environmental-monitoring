package org.nep.nepsystem.dto;

import java.util.List;

/**
 * 监督事件详情 VO（Phase 2 新增）：事件 + 附件列表 + 状态流转日志
 */
public class SupervisionDetailVO {
    private SupervisionEventVO event;
    private List<SupervisionAttachmentDTO> attachments;
    private List<SupervisionStatusLogVO> statusLogs;
    /** 关联巡检任务ID（Phase 6 新增，可空=未派单） */
    private Long taskId;
    /** 关联巡检任务编号（Phase 6 新增，可空） */
    private String taskNo;

    public SupervisionEventVO getEvent() { return event; }
    public void setEvent(SupervisionEventVO event) { this.event = event; }
    public List<SupervisionAttachmentDTO> getAttachments() { return attachments; }
    public void setAttachments(List<SupervisionAttachmentDTO> attachments) { this.attachments = attachments; }
    public List<SupervisionStatusLogVO> getStatusLogs() { return statusLogs; }
    public void setStatusLogs(List<SupervisionStatusLogVO> statusLogs) { this.statusLogs = statusLogs; }
    public Long getTaskId() { return taskId; }
    public void setTaskId(Long taskId) { this.taskId = taskId; }
    public String getTaskNo() { return taskNo; }
    public void setTaskNo(String taskNo) { this.taskNo = taskNo; }
}