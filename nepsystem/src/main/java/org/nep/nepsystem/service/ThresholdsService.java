package org.nep.nepsystem.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import org.nep.nepsystem.bean.Thresholds;

/**
 * 告警阈值业务：Thresholds 表的业务接口
 */
public interface ThresholdsService {

    /** 新增记录 */
    boolean save(Thresholds entity);

    /** 按主键修改记录 */
    boolean update(Thresholds entity);

    /** 按主键删除记录 */
    boolean deleteById(java.io.Serializable id);

    /** 按主键查询记录 */
    Thresholds getById(java.io.Serializable id);

    /** 分页查询（page 从 1 开始） */
    IPage<Thresholds> page(int page, int size);
}