package org.nep.nepsystem.bean;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.math.BigDecimal;
import java.util.Date;

/**
 * event_evaluation 表实体
 */
@TableName(value = "event_evaluation")
public class EventEvaluation {

    /** 评价ID(主键) */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 监督事件ID */
    @TableField(value = "event_id")
    private Long eventId;

    /** 评价人ID(可空=匿名) */
    @TableField(value = "user_id")
    private Integer userId;

    /** 评分 1-5 */
    @TableField(value = "score")
    private Integer score;

    /** 评价内容 */
    @TableField(value = "content")
    private String content;

    /** 评价时间 */
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

    public Integer getUserId() {
        return userId;
    }

    public void setUserId(Integer userId) {
        this.userId = userId;
    }

    public Integer getScore() {
        return score;
    }

    public void setScore(Integer score) {
        this.score = score;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public Date getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }
}
