package org.nep.nepsystem.dto;

/**
 * 网格员分配请求体（Phase 4 新增）：{gridId, userId, role}
 * role: GRID_USER网格员 / GRID_LEADER网格长（缺省 GRID_USER）
 */
public class GridMemberAssignDTO {
    private Integer gridId;
    private Integer userId;
    private String role;

    public Integer getGridId() { return gridId; }
    public void setGridId(Integer gridId) { this.gridId = gridId; }
    public Integer getUserId() { return userId; }
    public void setUserId(Integer userId) { this.userId = userId; }
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
}
