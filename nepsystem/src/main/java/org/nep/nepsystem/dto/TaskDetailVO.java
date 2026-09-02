package org.nep.nepsystem.dto;

import java.util.List;

/**
 * 网格员任务详情 VO（Phase 5 新增）：任务 + 公众反馈（关联监督事件及其附件）
 */
public class TaskDetailVO {
    private TaskVO task;
    private SupervisionEventVO event;
    private List<SupervisionAttachmentDTO> eventAttachments;

    public TaskVO getTask() { return task; }
    public void setTask(TaskVO task) { this.task = task; }
    public SupervisionEventVO getEvent() { return event; }
    public void setEvent(SupervisionEventVO event) { this.event = event; }
    public List<SupervisionAttachmentDTO> getEventAttachments() { return eventAttachments; }
    public void setEventAttachments(List<SupervisionAttachmentDTO> eventAttachments) { this.eventAttachments = eventAttachments; }
}
