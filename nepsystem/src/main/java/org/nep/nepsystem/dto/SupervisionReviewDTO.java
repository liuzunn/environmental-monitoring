package org.nep.nepsystem.dto;

/**
 * 审核请求体（Phase 2 新增）：{ "remark": "审核意见" }（可省略）
 * 用于 /api/supervision/{id}/approve|reject
 */
public class SupervisionReviewDTO {
    private String remark;

    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }
}
