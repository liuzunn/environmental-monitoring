package org.nep.nepsystem.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import org.nep.nepsystem.bean.MonitorData;

/**
 * 监测数据业务：MonitorData 表的业务接口
 */
public interface MonitorDataService {

    /** 新增记录 */
    boolean save(MonitorData entity);

    /** 按主键修改记录 */
    boolean update(MonitorData entity);

    /** 按主键删除记录 */
    boolean deleteById(java.io.Serializable id);

    /** 按主键查询记录 */
    MonitorData getById(java.io.Serializable id);

    /** 分页查询（page 从 1 开始） */
    IPage<MonitorData> page(int page, int size);

    /**
     * 数据上报（核心事务）：写入监测数据 + 更新设备在线状态 + 触发阈值告警
     * @param deviceCode 设备编号
     * @param items      指标列表 [{sensorCode, value}]
     * @param reportTime 上报时间，null 时取当前时间
     */
    void report(String deviceCode, java.util.List<java.util.Map<String, Object>> items, java.util.Date reportTime);
}