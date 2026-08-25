package org.nep.nepsystem.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.nep.nepsystem.bean.Thresholds;
import org.nep.nepsystem.dao.ThresholdsDao;
import org.nep.nepsystem.service.ThresholdsService;
import org.springframework.stereotype.Service;

/**
 * 告警阈值业务实现
 */
@Service
public class ThresholdsServiceImpl extends ServiceImpl<ThresholdsDao, Thresholds> implements ThresholdsService {

    @Override
    public boolean save(Thresholds entity) {
        return super.save(entity);
    }

    @Override
    public boolean update(Thresholds entity) {
        return super.updateById(entity);
    }

    @Override
    public boolean deleteById(java.io.Serializable id) {
        return super.removeById(id);
    }

    @Override
    public Thresholds getById(java.io.Serializable id) {
        return super.getById(id);
    }

    @Override
    public IPage<Thresholds> page(int page, int size) {
        return this.page(new Page<>(page, size), new QueryWrapper<>());
    }
}