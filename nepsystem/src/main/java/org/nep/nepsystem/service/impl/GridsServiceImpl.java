package org.nep.nepsystem.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.nep.nepsystem.bean.GridMember;
import org.nep.nepsystem.bean.Grids;
import org.nep.nepsystem.bean.InspectionTask;
import org.nep.nepsystem.dao.AdminsDao;
import org.nep.nepsystem.dao.GridMemberDao;
import org.nep.nepsystem.dao.GridsDao;
import org.nep.nepsystem.dao.InspectionTaskDao;
import org.nep.nepsystem.exception.BizException;
import org.nep.nepsystem.service.GridsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Date;
import java.util.List;

/**
 * 网格管理业务实现（Phase 4 新增）
 */
@Service
public class GridsServiceImpl extends ServiceImpl<GridsDao, Grids> implements GridsService {

    @Autowired private AdminsDao adminsDao;
    @Autowired private GridMemberDao gridMemberDao;
    @Autowired private InspectionTaskDao inspectionTaskDao;

    private void requireAdmin(Integer adminId) {
        if (adminId == null || adminsDao.selectById(adminId) == null) {
            throw new BizException(403, "管理员不存在或无权限");
        }
    }

    @Override
    public List<Grids> list(Integer adminId, String keyword, Integer status) {
        requireAdmin(adminId);
        QueryWrapper<Grids> qw = new QueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            qw.and(w -> w.like("grid_code", keyword).or().like("grid_name", keyword));
        }
        if (status != null) {
            qw.eq("status", status);
        }
        qw.orderByAsc("id");
        return super.list(qw);
    }

    @Override
    public Grids add(Integer adminId, Grids grid) {
        requireAdmin(adminId);
        if (grid == null || !StringUtils.hasText(grid.getGridCode()) || !StringUtils.hasText(grid.getGridName())) {
            throw new BizException(400, "网格编号和名称不能为空");
        }
        long cnt = super.count(new QueryWrapper<Grids>().eq("grid_code", grid.getGridCode()));
        if (cnt > 0) {
            throw new BizException("网格编号已存在: " + grid.getGridCode());
        }
        if (grid.getStatus() == null) {
            grid.setStatus(1);
        }
        grid.setId(null);
        grid.setCreateTime(new Date());
        super.save(grid);
        return grid;
    }

    @Override
    public Grids update(Integer adminId, Grids grid) {
        requireAdmin(adminId);
        if (grid == null || grid.getId() == null) {
            throw new BizException(400, "缺少网格ID");
        }
        if (super.getById(grid.getId()) == null) {
            throw new BizException(400, "网格不存在: " + grid.getId());
        }
        grid.setCreateTime(null);
        super.updateById(grid);
        return grid;
    }

    @Override
    public void delete(Integer adminId, Integer id) {
        requireAdmin(adminId);
        if (id == null || super.getById(id) == null) {
            throw new BizException(400, "网格不存在: " + id);
        }
        long members = gridMemberDao.selectCount(new QueryWrapper<GridMember>().eq("grid_id", id));
        if (members > 0) {
            throw new BizException("该网格下有 " + members + " 名成员，请先移除成员或停用网格");
        }
        long tasks = inspectionTaskDao.selectCount(new QueryWrapper<InspectionTask>().eq("grid_id", id));
        if (tasks > 0) {
            throw new BizException("该网格关联 " + tasks + " 个任务，请停用网格");
        }
        super.removeById(id);
    }

    @Override
    public Grids changeStatus(Integer adminId, Integer id, Integer status) {
        requireAdmin(adminId);
        if (id == null || super.getById(id) == null) {
            throw new BizException(400, "网格不存在: " + id);
        }
        if (status == null || (status != 0 && status != 1)) {
            throw new BizException(400, "非法状态: " + status);
        }
        Grids g = new Grids();
        g.setId(id);
        g.setStatus(status);
        super.updateById(g);
        return super.getById(id);
    }
}