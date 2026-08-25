package org.nep.nepsystem.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.nep.nepsystem.bean.Sensors;
import org.nep.nepsystem.dao.SensorsDao;
import org.nep.nepsystem.service.SensorsService;
import org.springframework.stereotype.Service;

/**
 * 监测指标字典业务实现
 */
@Service
public class SensorsServiceImpl extends ServiceImpl<SensorsDao, Sensors> implements SensorsService {

    @Override
    public boolean save(Sensors entity) {
        return super.save(entity);
    }

    @Override
    public boolean update(Sensors entity) {
        return super.updateById(entity);
    }

    @Override
    public boolean deleteById(java.io.Serializable id) {
        return super.removeById(id);
    }

    @Override
    public Sensors getById(java.io.Serializable id) {
        return super.getById(id);
    }

    @Override
    public IPage<Sensors> page(int page, int size) {
        return this.page(new Page<>(page, size), new QueryWrapper<>());
    }
}