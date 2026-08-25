package org.nep.nepsystem.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.nep.nepsystem.bean.Devices;
import org.nep.nepsystem.dao.DevicesDao;
import org.nep.nepsystem.service.DevicesService;
import org.springframework.stereotype.Service;

/**
 * 监测设备管理业务实现
 */
@Service
public class DevicesServiceImpl extends ServiceImpl<DevicesDao, Devices> implements DevicesService {

    @Override
    public boolean save(Devices entity) {
        return super.save(entity);
    }

    @Override
    public boolean update(Devices entity) {
        return super.updateById(entity);
    }

    @Override
    public boolean deleteById(java.io.Serializable id) {
        return super.removeById(id);
    }

    @Override
    public Devices getById(java.io.Serializable id) {
        return super.getById(id);
    }

    @Override
    public IPage<Devices> page(int page, int size) {
        return this.page(new Page<>(page, size), new QueryWrapper<>());
    }
}