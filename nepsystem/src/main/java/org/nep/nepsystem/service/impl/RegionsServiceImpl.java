package org.nep.nepsystem.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.nep.nepsystem.bean.Regions;
import org.nep.nepsystem.dao.RegionsDao;
import org.nep.nepsystem.service.RegionsService;
import org.springframework.stereotype.Service;

/**
 * 区域管理业务实现
 */
@Service
public class RegionsServiceImpl extends ServiceImpl<RegionsDao, Regions> implements RegionsService {

    @Override
    public boolean save(Regions entity) {
        return super.save(entity);
    }

    @Override
    public boolean update(Regions entity) {
        return super.updateById(entity);
    }

    @Override
    public boolean deleteById(java.io.Serializable id) {
        return super.removeById(id);
    }

    @Override
    public Regions getById(java.io.Serializable id) {
        return super.getById(id);
    }

    @Override
    public IPage<Regions> page(int page, int size) {
        return this.page(new Page<>(page, size), new QueryWrapper<>());
    }
}