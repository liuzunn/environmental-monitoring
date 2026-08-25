package org.nep.nepsystem.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import org.nep.nepsystem.bean.Regions;

/**
 * 区域管理业务：Regions 表的业务接口
 */
public interface RegionsService {

    /** 新增记录 */
    boolean save(Regions entity);

    /** 按主键修改记录 */
    boolean update(Regions entity);

    /** 按主键删除记录 */
    boolean deleteById(java.io.Serializable id);

    /** 按主键查询记录 */
    Regions getById(java.io.Serializable id);

    /** 分页查询（page 从 1 开始） */
    IPage<Regions> page(int page, int size);
}