package org.nep.nepsystem.bean;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.math.BigDecimal;
import java.util.Date;

/**
 * supervision_attachment 表实体
 */
@TableName(value = "supervision_attachment")
public class SupervisionAttachment {

    /** 附件ID(主键) */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 监督事件ID */
    @TableField(value = "event_id")
    @com.fasterxml.jackson.databind.annotation.JsonSerialize(using = com.fasterxml.jackson.databind.ser.std.ToStringSerializer.class)
    private Long eventId;

    /** 原始文件名 */
    @TableField(value = "file_name")
    private String fileName;

    /** 存储路径 */
    @TableField(value = "file_path")
    private String filePath;

    /** 文件大小(字节) */
    @TableField(value = "file_size")
    private Long fileSize;

    /** MIME类型 */
    @TableField(value = "content_type")
    private String contentType;

    /** 上传人ID(可空) */
    @TableField(value = "upload_user_id")
    private Integer uploadUserId;

    /** 上传时间 */
    @TableField(value = "create_time")
    private Date createTime;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getEventId() {
        return eventId;
    }

    public void setEventId(Long eventId) {
        this.eventId = eventId;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public Long getFileSize() {
        return fileSize;
    }

    public void setFileSize(Long fileSize) {
        this.fileSize = fileSize;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public Integer getUploadUserId() {
        return uploadUserId;
    }

    public void setUploadUserId(Integer uploadUserId) {
        this.uploadUserId = uploadUserId;
    }

    public Date getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }
}
