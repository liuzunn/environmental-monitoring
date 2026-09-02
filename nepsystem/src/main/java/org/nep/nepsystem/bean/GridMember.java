package org.nep.nepsystem.bean;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.math.BigDecimal;
import java.util.Date;

/**
 * grid_member 表实体
 */
@TableName(value = "grid_member")
public class GridMember {

    /** 成员ID(主键) */
    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    /** 网格ID */
    @TableField(value = "grid_id")
    private Integer gridId;

    /** 用户ID(网格员) */
    @TableField(value = "user_id")
    private Integer userId;

    /** 网格内角色: GRID_USER/GRID_LEADER */
    @TableField(value = "role")
    private String role;

    /** 状态: 1在职 0离职 */
    @TableField(value = "status")
    private Integer status;

    /** 加入时间 */
    @TableField(value = "create_time")
    private Date createTime;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getGridId() {
        return gridId;
    }

    public void setGridId(Integer gridId) {
        this.gridId = gridId;
    }

    public Integer getUserId() {
        return userId;
    }

    public void setUserId(Integer userId) {
        this.userId = userId;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public Date getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }
}
