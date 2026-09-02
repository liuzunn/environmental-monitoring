package org.nep.nepsystem.dto;

import java.math.BigDecimal;
import java.util.List;

/**
 * 现场检测提交请求体（Phase 5 新增，网格员）：
 * 六项污染物（缺项不参与AQI计算）+ 备注 + 照片文件名列表 + 检测坐标
 * AQI 由后端按 HJ633-2012 1小时均值分段表计算
 */
public class DetectSubmitDTO {
    private BigDecimal pm25;
    private BigDecimal pm10;
    private BigDecimal so2;
    private BigDecimal no2;
    private BigDecimal co;
    private BigDecimal o3;
    private String content;
    private List<String> images;
    private BigDecimal lat;
    private BigDecimal lng;

    public BigDecimal getPm25() { return pm25; }
    public void setPm25(BigDecimal pm25) { this.pm25 = pm25; }
    public BigDecimal getPm10() { return pm10; }
    public void setPm10(BigDecimal pm10) { this.pm10 = pm10; }
    public BigDecimal getSo2() { return so2; }
    public void setSo2(BigDecimal so2) { this.so2 = so2; }
    public BigDecimal getNo2() { return no2; }
    public void setNo2(BigDecimal no2) { this.no2 = no2; }
    public BigDecimal getCo() { return co; }
    public void setCo(BigDecimal co) { this.co = co; }
    public BigDecimal getO3() { return o3; }
    public void setO3(BigDecimal o3) { this.o3 = o3; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public List<String> getImages() { return images; }
    public void setImages(List<String> images) { this.images = images; }
    public BigDecimal getLat() { return lat; }
    public void setLat(BigDecimal lat) { this.lat = lat; }
    public BigDecimal getLng() { return lng; }
    public void setLng(BigDecimal lng) { this.lng = lng; }
}
