package org.nep.nepsystem.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import org.nep.nepsystem.bean.Devices;

/**
 * 监测设备管理业务：Devices 表的业务接口
 */
public interface DevicesService {

    /** 新增记录 */
    boolean save(Devices entity);

    /** 按主键修改记录 */
    boolean update(Devices entity);

    /** 按主键删除记录 */
    boolean deleteById(java.io.Serializable id);

    /** 按主键查询记录 */
    Devices getById(java.io.Serializable id);

    /** 分页查询（page 从 1 开始） */
    IPage<Devices> page(int page, int size);
}