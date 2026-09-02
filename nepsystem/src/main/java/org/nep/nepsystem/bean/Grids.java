package org.nep.nepsystem.bean;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.math.BigDecimal;
import java.util.Date;

/**
 * grids 表实体
 */
@TableName(value = "grids")
public class Grids {

    /** 网格ID(主键) */
    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    /** 网格编号(唯一) */
    @TableField(value = "grid_code")
    private String gridCode;

    /** 网格名称 */
    @TableField(value = "grid_name")
    private String gridName;

    /** 所属区域ID */
    @TableField(value = "region_id")
    private Integer regionId;

    /** 网格描述 */
    @TableField(value = "description")
    private String description;

    /** 状态: 1启用 0停用（v5 新增） */
    @TableField(value = "status")
    private Integer status;

    /** 创建时间 */
    @TableField(value = "create_time")
    private Date createTime;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getGridCode() {
        return gridCode;
    }

    public void setGridCode(String gridCode) {
        this.gridCode = gridCode;
    }

    public String getGridName() {
        return gridName;
    }

    public void setGridName(String gridName) {
        this.gridName = gridName;
    }

    public Integer getRegionId() {
        return regionId;
    }

    public void setRegionId(Integer regionId) {
        this.regionId = regionId;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
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
