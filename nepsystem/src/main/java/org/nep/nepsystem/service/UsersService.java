package org.nep.nepsystem.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import org.nep.nepsystem.bean.Users;

/**
 * 用户管理业务：Users 表的业务接口
 */
public interface UsersService {

    /** 新增记录 */
    boolean save(Users entity);

    /** 按主键修改记录 */
    boolean update(Users entity);

    /** 按主键删除记录 */
    boolean deleteById(java.io.Serializable id);

    /** 按主键查询记录 */
    Users getById(java.io.Serializable id);

    /** 分页查询（page 从 1 开始） */
    IPage<Users> page(int page, int size);
}