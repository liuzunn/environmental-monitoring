package org.nep.nepsystem.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.nep.nepsystem.bean.Users;
import org.nep.nepsystem.dao.UsersDao;
import org.nep.nepsystem.service.UsersService;
import org.springframework.stereotype.Service;

/**
 * 用户管理业务实现
 */
@Service
public class UsersServiceImpl extends ServiceImpl<UsersDao, Users> implements UsersService {

    @Override
    public boolean save(Users entity) {
        return super.save(entity);
    }

    @Override
    public boolean update(Users entity) {
        return super.updateById(entity);
    }

    @Override
    public boolean deleteById(java.io.Serializable id) {
        return super.removeById(id);
    }

    @Override
    public Users getById(java.io.Serializable id) {
        return super.getById(id);
    }

    @Override
    public IPage<Users> page(int page, int size) {
        return this.page(new Page<>(page, size), new QueryWrapper<>());
    }
}