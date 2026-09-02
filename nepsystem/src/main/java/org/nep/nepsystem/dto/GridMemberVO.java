package org.nep.nepsystem.dto;

import java.util.Date;

/**
 * 网格成员 VO（Phase 4 新增）：成员 + 用户/网格名称
 */
public class GridMemberVO {
    private Integer id;
    private Integer gridId;
    private String gridName;
    private Integer userId;
    private String username;
    private String nickname;
    private String role;
    private Integer status;
    private Date createTime;

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public Integer getGridId() { return gridId; }
    public void setGridId(Integer gridId) { this.gridId = gridId; }
    public String getGridName() { return gridName; }
    public void setGridName(String gridName) { this.gridName = gridName; }
    public Integer getUserId() { return userId; }
    public void setUserId(Integer userId) { this.userId = userId; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getNickname() { return nickname; }
    public void setNickname(String nickname) { this.nickname = nickname; }
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }
    public Date getCreateTime() { return createTime; }
    public void setCreateTime(Date createTime) { this.createTime = createTime; }
}
