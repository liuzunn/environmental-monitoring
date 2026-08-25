package org.nep.nepsystem.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.nep.nepsystem.bean.Alerts;
import org.nep.nepsystem.dao.AlertsDao;
import org.nep.nepsystem.service.AlertsService;
import org.springframework.stereotype.Service;

/**
 * 告警记录业务实现
 */
@Service
public class AlertsServiceImpl extends ServiceImpl<AlertsDao, Alerts> implements AlertsService {

    @Override
    public boolean save(Alerts entity) {
        return super.save(entity);
    }

    @Override
    public boolean update(Alerts entity) {
        return super.updateById(entity);
    }

    @Override
    public boolean deleteById(java.io.Serializable id) {
        return super.removeById(id);
    }

    @Override
    public Alerts getById(java.io.Serializable id) {
        return super.getById(id);
    }

    @Override
    public IPage<Alerts> page(int page, int size) {
        return this.page(new Page<>(page, size), new QueryWrapper<>());
    }
}