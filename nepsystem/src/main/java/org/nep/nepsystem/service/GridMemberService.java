package org.nep.nepsystem.service;

import org.nep.nepsystem.bean.GridMember;
import org.nep.nepsystem.dto.GridMemberAssignDTO;
import org.nep.nepsystem.dto.GridMemberVO;

import java.util.List;

/**
 * 网格成员管理业务（Phase 4 新增）：网格员分配/移除/查询
 */
public interface GridMemberService {

    /** 网格员列表（可按网格过滤/关键字，含用户与网格名称） */
    List<GridMemberVO> list(Integer adminId, Integer gridId, String keyword);

    /** 分配网格员到网格（唯一键 grid_id+user_id） */
    GridMember assign(Integer adminId, GridMemberAssignDTO dto);

    /** 移除网格成员 */
    void remove(Integer adminId, Integer id);
}
