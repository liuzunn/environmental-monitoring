package org.nep.nepsystem.ctrl;

import org.nep.nepsystem.bean.Grids;
import org.nep.nepsystem.common.Result;
import org.nep.nepsystem.service.GridsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 网格管理接口（Phase 4 新增，NEPM 管理员端）：
 * 全部接口需请求头 X-Admin-Id
 */
@RestController
@RequestMapping("/api/grids")
public class GridsController {

    @Autowired
    private GridsService gridsService;

    @GetMapping("/list")
    public Result<List<Grids>> list(@RequestHeader(value = "X-Admin-Id", required = false) Integer adminId,
                                    @RequestParam(required = false) String keyword,
                                    @RequestParam(required = false) Integer status) {
        return Result.ok(gridsService.list(adminId, keyword, status));
    }

    @PostMapping
    public Result<Grids> add(@RequestHeader(value = "X-Admin-Id", required = false) Integer adminId,
                             @RequestBody Grids grid) {
        return Result.ok("新增成功", gridsService.add(adminId, grid));
    }

    @PutMapping
    public Result<Grids> update(@RequestHeader(value = "X-Admin-Id", required = false) Integer adminId,
                                @RequestBody Grids grid) {
        return Result.ok("修改成功", gridsService.update(adminId, grid));
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@RequestHeader(value = "X-Admin-Id", required = false) Integer adminId,
                               @PathVariable Integer id) {
        gridsService.delete(adminId, id);
        return Result.ok("删除成功", null);
    }

    @PutMapping("/{id}/status")
    public Result<Grids> changeStatus(@RequestHeader(value = "X-Admin-Id", required = false) Integer adminId,
                                      @PathVariable Integer id,
                                      @RequestParam Integer status) {
        return Result.ok(status == 1 ? "已启用" : "已停用", gridsService.changeStatus(adminId, id, status));
    }
}
