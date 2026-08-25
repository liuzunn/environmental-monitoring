package org.nep.nepsystem.bean;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
/*
实体类----数据库表
属性----数据库表的字段
属性实现封装
 */
@TableName(value = "admins")
public class Admins {
    @TableId(value = "admin_id", type = IdType.AUTO)
    private int adminId;
    @TableField(value = "admin_code")
    private String adminCode;

    @TableField(value = "password")
    private String password;

    @TableField(value = "remarks")
    private String remarks;

    public int getAdminId() {
        return adminId;
    }

    public void setAdminId(int adminId) {
        this.adminId = adminId;
    }

    public String getAdminCode() {
        return adminCode;
    }

    public void setAdminCode(String adminCode) {
        this.adminCode = adminCode;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getRemarks() {
        return remarks;
    }

    public void setRemarks(String remarks) {
        this.remarks = remarks;
    }
}