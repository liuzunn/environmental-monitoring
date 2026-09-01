package org.nep.nepsystem.dto;

/** 异常检测汇总项（业务层升级新增） */
public class AnomalySummaryDTO {
    private String issueType;
    private Long count;

    public AnomalySummaryDTO() {
    }

    public AnomalySummaryDTO(String issueType, Long count) {
        this.issueType = issueType;
        this.count = count;
    }

    public String getIssueType() { return issueType; }
    public void setIssueType(String issueType) { this.issueType = issueType; }
    public Long getCount() { return count; }
    public void setCount(Long count) { this.count = count; }
}
