package org.nep.nepsystem.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import org.nep.nepsystem.bean.Sensors;

/**
 * 监测指标字典业务：Sensors 表的业务接口
 */
public interface SensorsService {

    /** 新增记录 */
    boolean save(Sensors entity);

    /** 按主键修改记录 */
    boolean update(Sensors entity);

    /** 按主键删除记录 */
    boolean deleteById(java.io.Serializable id);

    /** 按主键查询记录 */
    Sensors getById(java.io.Serializable id);

    /** 分页查询（page 从 1 开始） */
    IPage<Sensors> page(int page, int size);
}