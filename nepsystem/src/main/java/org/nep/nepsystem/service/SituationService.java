package org.nep.nepsystem.service;

import org.nep.nepsystem.dto.DeviceSituationDTO;
import org.nep.nepsystem.dto.SituationOverviewDTO;

import java.util.List;

/**
 * 空间态势服务（空间态势升级新增）：
 * 聚合 设备/最新数据/活跃告警/健康度/数据质量 为态势视图数据源。
 * 兼容性：仅新增接口，不动既有设备/数据接口。
 */
public interface SituationService {

    /** 态势总览：设备总数/在线/离线/预警设备数/报警设备数/活跃告警数/环境状态 */
    SituationOverviewDTO overview();

    /** 态势设备列表（含最新数据/活跃告警/健康度），支持 keyword/type/status/alertLevel 过滤 */
    List<DeviceSituationDTO> devices(String keyword, String type, Integer status, String alertLevel);
}
