package org.nep.nepsystem.ctrl;

import org.nep.nepsystem.bean.InspectionTask;
import org.nep.nepsystem.common.PageResult;
import org.nep.nepsystem.common.Result;
import org.nep.nepsystem.dto.AssigneeStatsVO;
import org.nep.nepsystem.dto.InspectionTaskCreateDTO;
import org.nep.nepsystem.dto.TaskVO;
import org.nep.nepsystem.dto.TaskDetailVO;
import org.nep.nepsystem.dto.TaskStatsVO;
import org.nep.nepsystem.dto.DetectSubmitDTO;
import org.nep.nepsystem.dto.InspectionRecordVO;
import org.nep.nepsystem.service.InspectionTaskService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 巡检任务接口（Phase 4 新增，NEPM 管理员端）：
 * 全部接口需请求头 X-Admin-Id
 */
@RestController
@RequestMapping("/api/tasks")
public class InspectionTaskController {

    @Autowired
    private InspectionTaskService inspectionTaskService;

    @GetMapping("/page")
    public Result<PageResult<TaskVO>> page(@RequestHeader(value = "X-Admin-Id", required = false) Integer adminId,
                                           @RequestParam(defaultValue = "1") int page,
                                           @RequestParam(defaultValue = "10") int size,
                                           @RequestParam(required = false) String status,
                                           @RequestParam(required = false) Integer assigneeId,
                                           @RequestParam(required = false) Integer gridId,
                                           @RequestParam(required = false) String keyword) {
        return Result.ok(inspectionTaskService.page(adminId, page, size, status, assigneeId, gridId, keyword));
    }

    @PostMapping
    public Result<InspectionTask> create(@RequestHeader(value = "X-Admin-Id", required = false) Integer adminId,
                                         @RequestBody InspectionTaskCreateDTO dto) {
        return Result.ok("创建成功", inspectionTaskService.create(adminId, dto));
    }

    @GetMapping("/assignee-stats")
    public Result<List<AssigneeStatsVO>> assigneeStats(@RequestHeader(value = "X-Admin-Id", required = false) Integer adminId) {
        return Result.ok(inspectionTaskService.assigneeStats(adminId));
    }

    // ===== Phase 5 网格员端（身份 X-User-Id） =====

    /** 我的任务（网格员，分页）：GET /api/tasks/mine */
    @GetMapping("/mine")
    public Result<PageResult<TaskVO>> mine(@RequestHeader(value = "X-User-Id", required = false) Integer userId,
                                           @RequestParam(defaultValue = "1") int page,
                                           @RequestParam(defaultValue = "10") int size,
                                           @RequestParam(required = false) String status,
                                           @RequestParam(required = false) String keyword) {
        return Result.ok(inspectionTaskService.pageMine(userId, page, size, status, keyword));
    }

    /** 我的任务统计（网格员）：GET /api/tasks/mine/stats */
    @GetMapping("/mine/stats")
    public Result<TaskStatsVO> mineStats(@RequestHeader(value = "X-User-Id", required = false) Integer userId) {
        return Result.ok(inspectionTaskService.mineStats(userId));
    }

    /** 任务详情（网格员，含公众反馈）：GET /api/tasks/mine/{id} */
    @GetMapping("/mine/{id}")
    public Result<TaskDetailVO> mineDetail(@PathVariable Long id,
                                           @RequestHeader(value = "X-User-Id", required = false) Integer userId) {
        return Result.ok(inspectionTaskService.detailMine(id, userId));
    }

    /** 接收任务：PUT /api/tasks/mine/{id}/accept（ASSIGNED -> ACCEPTED） */
    @PutMapping("/mine/{id}/accept")
    public Result<InspectionTask> accept(@PathVariable Long id,
                                         @RequestHeader(value = "X-User-Id", required = false) Integer userId) {
        return Result.ok("已接收任务", inspectionTaskService.accept(id, userId));
    }

    /** 开始检测：PUT /api/tasks/mine/{id}/start（ACCEPTED -> INSPECTING） */
    @PutMapping("/mine/{id}/start")
    public Result<InspectionTask> start(@PathVariable Long id,
                                        @RequestHeader(value = "X-User-Id", required = false) Integer userId) {
        return Result.ok("开始检测", inspectionTaskService.start(id, userId));
    }

    /** 提交检测：PUT /api/tasks/mine/{id}/submit（INSPECTING -> INSPECTED + inspection_record） */
    @PutMapping("/mine/{id}/submit")
    public Result<InspectionTask> submit(@PathVariable Long id,
                                         @RequestHeader(value = "X-User-Id", required = false) Integer userId,
                                         @RequestBody(required = false) DetectSubmitDTO dto) {
        InspectionTask t = inspectionTaskService.submit(id, userId, dto);
        return Result.ok("检测已提交", t);
    }

    // ===== Phase 6 闭环（管理员） =====

    /** 查看任务检测记录（管理员）：GET /api/tasks/{id}/records */
    @GetMapping("/{id}/records")
    public Result<List<InspectionRecordVO>> records(@RequestHeader(value = "X-Admin-Id", required = false) Integer adminId,
                                                    @PathVariable Long id) {
        return Result.ok(inspectionTaskService.records(adminId, id));
    }

    /** 核实检测结果（管理员）：PUT /api/tasks/{id}/verify（INSPECTED -> VERIFIED，事件同步） */
    @PutMapping("/{id}/verify")
    public Result<InspectionTask> verify(@RequestHeader(value = "X-Admin-Id", required = false) Integer adminId,
                                         @PathVariable Long id,
                                         @RequestParam(required = false) String remark) {
        return Result.ok("已核实", inspectionTaskService.verify(adminId, id, remark));
    }

    /** 关闭任务/事件（管理员）：PUT /api/tasks/{id}/close（VERIFIED -> CLOSED，事件同步） */
    @PutMapping("/{id}/close")
    public Result<InspectionTask> close(@RequestHeader(value = "X-Admin-Id", required = false) Integer adminId,
                                        @PathVariable Long id,
                                        @RequestParam(required = false) String remark) {
        return Result.ok("已关闭", inspectionTaskService.close(adminId, id, remark));
    }
}