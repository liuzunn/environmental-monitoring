package org.nep.nepsystem.bean;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;

import java.math.BigDecimal;
import java.util.Date;

/**
 * users 表实体
 */
@TableName(value = "users")
public class Users {

    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    /** 用户名(登录名) */
    @TableField(value = "username")
    private String username;

    /** 密码 */
    @TableField(value = "password")
    private String password;

    /** 昵称 */
    @TableField(value = "nickname")
    private String nickname;

    /** 角色: ADMIN/USER */
    @TableField(value = "role")
    private String role;

    /** 状态: 1启用 0禁用 */
    @TableField(value = "status")
    private Integer status;

    /** 创建时间 */
    @TableField(value = "create_time")
    private java.util.Date createTime;


    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
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

    public java.util.Date getCreateTime() {
        return createTime;
    }

    public void setCreateTime(java.util.Date createTime) {
        this.createTime = createTime;
    }

}