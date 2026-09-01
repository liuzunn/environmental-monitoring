package org.nep.nepsystem.dto;

/**
 * 告警流转请求体：{ "user": "操作人" }（可省略，缺省 admin）
 * 用于 /api/alerts/{id}/acknowledge|process|resolve
 */
public class AlertActionDTO {
    private String user;

    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
    }
}
