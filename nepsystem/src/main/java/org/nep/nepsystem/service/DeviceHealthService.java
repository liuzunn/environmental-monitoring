package org.nep.nepsystem.service;

import org.nep.nepsystem.dto.DeviceHealthDTO;

import java.util.List;

/**
 * 设备健康度服务（业务层升级）：
 * 基于现有表（devices/monitor_data/data_quality/alerts）实时计算，无需新增表。
 * 权重：在线率 25% + 通信时效 25% + 数据完整率 20% + 异常 15% + 告警 15%。
 */
public interface DeviceHealthService {

    /** 计算单台设备健康度 */
    DeviceHealthDTO calculate(Integer deviceId);

    /** 计算全部设备健康度 */
    List<DeviceHealthDTO> calculateAll();
}
