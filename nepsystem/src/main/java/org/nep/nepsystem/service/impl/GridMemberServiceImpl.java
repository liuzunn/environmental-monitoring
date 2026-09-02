package org.nep.nepsystem.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.nep.nepsystem.bean.GridMember;
import org.nep.nepsystem.bean.Grids;
import org.nep.nepsystem.bean.Users;
import org.nep.nepsystem.dao.AdminsDao;
import org.nep.nepsystem.dao.GridMemberDao;
import org.nep.nepsystem.dao.GridsDao;
import org.nep.nepsystem.dao.UsersDao;
import org.nep.nepsystem.dto.GridMemberAssignDTO;
import org.nep.nepsystem.dto.GridMemberVO;
import org.nep.nepsystem.exception.BizException;
import org.nep.nepsystem.service.GridMemberService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 网格成员管理业务实现（Phase 4 新增）
 */
@Service
public class GridMemberServiceImpl extends ServiceImpl<GridMemberDao, GridMember> implements GridMemberService {

    @Autowired private AdminsDao adminsDao;
    @Autowired private GridsDao gridsDao;
    @Autowired private UsersDao usersDao;

    private void requireAdmin(Integer adminId) {
        if (adminId == null || adminsDao.selectById(adminId) == null) {
            throw new BizException(403, "管理员不存在或无权限");
        }
    }

    @Override
    public List<GridMemberVO> list(Integer adminId, Integer gridId, String keyword) {
        requireAdmin(adminId);
        QueryWrapper<GridMember> qw = new QueryWrapper<>();
        if (gridId != null) {
            qw.eq("grid_id", gridId);
        }
        qw.orderByAsc("id");
        List<GridMember> rows = super.list(qw);
        // 批量名称
        Map<Integer, String> gridNames = gridsDao.selectBatchIds(rows.stream().map(GridMember::getGridId).distinct().collect(Collectors.toList()))
                .stream().collect(Collectors.toMap(Grids::getId, Grids::getGridName, (a, b) -> a));
        Map<Integer, Users> users = rows.stream().map(GridMember::getUserId).distinct()
                .map(id -> usersDao.selectById(id)).filter(u -> u != null)
                .collect(Collectors.toMap(Users::getId, u -> u, (a, b) -> a));
        List<GridMemberVO> out = new ArrayList<>();
        for (GridMember m : rows) {
            GridMemberVO vo = new GridMemberVO();
            vo.setId(m.getId());
            vo.setGridId(m.getGridId());
            vo.setGridName(gridNames.getOrDefault(m.getGridId(), "-"));
            vo.setUserId(m.getUserId());
            Users u = users.get(m.getUserId());
            if (u != null) {
                vo.setUsername(u.getUsername());
                vo.setNickname(u.getNickname());
            }
            vo.setRole(m.getRole());
            vo.setStatus(m.getStatus());
            vo.setCreateTime(m.getCreateTime());
            if (StringUtils.hasText(keyword)) {
                boolean hit = (u != null && (StringUtils.hasText(u.getUsername()) && u.getUsername().contains(keyword)
                        || (StringUtils.hasText(u.getNickname()) && u.getNickname().contains(keyword))));
                if (!hit) continue;
            }
            out.add(vo);
        }
        return out;
    }

    @Override
    public GridMember assign(Integer adminId, GridMemberAssignDTO dto) {
        requireAdmin(adminId);
        if (dto == null || dto.getGridId() == null || dto.getUserId() == null) {
            throw new BizException(400, "网格与用户不能为空");
        }
        Grids grid = gridsDao.selectById(dto.getGridId());
        if (grid == null || (grid.getStatus() != null && grid.getStatus() != 1)) {
            throw new BizException(400, "网格不存在或已停用: " + dto.getGridId());
        }
        Users user = usersDao.selectById(dto.getUserId());
        if (user == null || user.getStatus() == null || user.getStatus() != 1) {
            throw new BizException(400, "用户不存在或已禁用: " + dto.getUserId());
        }
        long exists = super.count(new QueryWrapper<GridMember>()
                .eq("grid_id", dto.getGridId()).eq("user_id", dto.getUserId()));
        if (exists > 0) {
            throw new BizException("该用户已在此网格中");
        }
        GridMember m = new GridMember();
        m.setGridId(dto.getGridId());
        m.setUserId(dto.getUserId());
        m.setRole(StringUtils.hasText(dto.getRole()) ? dto.getRole() : "GRID_USER");
        m.setStatus(1);
        m.setCreateTime(new Date());
        super.save(m);
        return m;
    }

    @Override
    public void remove(Integer adminId, Integer id) {
        requireAdmin(adminId);
        if (id == null || super.getById(id) == null) {
            throw new BizException(400, "成员记录不存在: " + id);
        }
        super.removeById(id);
    }
}