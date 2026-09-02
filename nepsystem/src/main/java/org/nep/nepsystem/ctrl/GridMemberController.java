package org.nep.nepsystem.ctrl;

import org.nep.nepsystem.bean.GridMember;
import org.nep.nepsystem.common.Result;
import org.nep.nepsystem.dto.GridMemberAssignDTO;
import org.nep.nepsystem.dto.GridMemberVO;
import org.nep.nepsystem.service.GridMemberService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 网格员管理接口（Phase 4 新增，NEPM 管理员端）：
 * 全部接口需请求头 X-Admin-Id
 */
@RestController
@RequestMapping("/api/grid-members")
public class GridMemberController {

    @Autowired
    private GridMemberService gridMemberService;

    @GetMapping("/list")
    public Result<List<GridMemberVO>> list(@RequestHeader(value = "X-Admin-Id", required = false) Integer adminId,
                                           @RequestParam(required = false) Integer gridId,
                                           @RequestParam(required = false) String keyword) {
        return Result.ok(gridMemberService.list(adminId, gridId, keyword));
    }

    @PostMapping("/assign")
    public Result<GridMember> assign(@RequestHeader(value = "X-Admin-Id", required = false) Integer adminId,
                                     @RequestBody GridMemberAssignDTO dto) {
        return Result.ok("分配成功", gridMemberService.assign(adminId, dto));
    }

    @DeleteMapping("/{id}")
    public Result<Void> remove(@RequestHeader(value = "X-Admin-Id", required = false) Integer adminId,
                               @PathVariable Integer id) {
        gridMemberService.remove(adminId, id);
        return Result.ok("已移除", null);
    }
}
