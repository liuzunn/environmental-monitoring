package org.nep.nepsystem.service;

import org.nep.nepsystem.bean.Grids;

import java.util.List;

/**
 * 网格管理业务（Phase 4 新增）：管理员维护网格（删除前校验引用，支持停用）
 */
public interface GridsService {

    /** 网格列表（keyword/status 过滤，全量） */
    List<Grids> list(Integer adminId, String keyword, Integer status);

    /** 新增网格（grid_code 唯一校验） */
    Grids add(Integer adminId, Grids grid);

    /** 编辑网格 */
    Grids update(Integer adminId, Grids grid);

    /** 删除网格（有成员/任务引用时拒绝，提示停用） */
    void delete(Integer adminId, Integer id);

    /** 停用/启用：status 1启用 0停用 */
    Grids changeStatus(Integer adminId, Integer id, Integer status);
}
