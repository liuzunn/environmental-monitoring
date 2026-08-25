package org.nep.nepsystem.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import org.nep.nepsystem.bean.Alerts;

/**
 * 告警记录业务：Alerts 表的业务接口
 */
public interface AlertsService {

    /** 新增记录 */
    boolean save(Alerts entity);

    /** 按主键修改记录 */
    boolean update(Alerts entity);

    /** 按主键删除记录 */
    boolean deleteById(java.io.Serializable id);

    /** 按主键查询记录 */
    Alerts getById(java.io.Serializable id);

    /** 分页查询（page 从 1 开始） */
    IPage<Alerts> page(int page, int size);
}