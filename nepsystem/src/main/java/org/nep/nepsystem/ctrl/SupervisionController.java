package org.nep.nepsystem.ctrl;

import org.nep.nepsystem.bean.SupervisionEvent;
import org.nep.nepsystem.common.PageResult;
import org.nep.nepsystem.common.Result;
import org.nep.nepsystem.dto.SupervisionCreateDTO;
import org.nep.nepsystem.dto.SupervisionDetailVO;
import org.nep.nepsystem.dto.SupervisionEventVO;
import java.util.List;
import org.nep.nepsystem.dto.AssignEventDTO;
import org.nep.nepsystem.dto.SupervisionReviewDTO;
import org.nep.nepsystem.dto.WorkbenchStatsDTO;
import org.nep.nepsystem.exception.BizException;
import org.nep.nepsystem.service.SupervisionEventService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

/**
 * 监督事件接口（Phase 2 新增）：
 * - 身份来源：公众 X-User-Id（users.id，创建/我的/本人详情）；
 *             管理员 X-Admin-Id（admins.admin_id，全部列表/详情/审核）
 * - 状态转换仅在 Service 内进行，前端不可传 status
 * - 分层：Controller -> Service -> Mapper（本 Controller 不注入任何 DAO）
 */
@RestController
@RequestMapping("/api/supervision")
public class SupervisionController {

    @Autowired
    private SupervisionEventService supervisionEventService;

    /** 解析请求头中的整数身份（缺失/非法返回 null） */
    private Integer headerId(String v) {
        if (!StringUtils.hasText(v)) {
            return null;
        }
        try {
            return Integer.valueOf(v.trim());
        } catch (NumberFormatException e) {
            throw new BizException(400, "请求头身份格式错误: " + v);
        }
    }

    /** 创建监督事件（公众）：POST /api/supervision */
    @PostMapping
    public Result<SupervisionEvent> create(@RequestHeader(value = "X-User-Id", required = false) String userIdHeader,
                                           @RequestBody(required = false) SupervisionCreateDTO dto) {
        return Result.ok("提交成功", supervisionEventService.create(headerId(userIdHeader), dto));
    }

    /** 我的监督事件（公众，分页）：GET /api/supervision/mine */
    @GetMapping("/mine")
    public Result<PageResult<SupervisionEventVO>> mine(@RequestHeader(value = "X-User-Id", required = false) String userIdHeader,
                                                       @RequestParam(defaultValue = "1") int page,
                                                       @RequestParam(defaultValue = "10") int size,
                                                       @RequestParam(required = false) String status) {
        return Result.ok(supervisionEventService.pageMine(headerId(userIdHeader), page, size, status));
    }

    /** 监督详情：GET /api/supervision/{id}（公众仅本人，管理员可全部） */
    @GetMapping("/{id}")
    public Result<SupervisionDetailVO> detail(@PathVariable Long id,
                                              @RequestHeader(value = "X-User-Id", required = false) String userIdHeader,
                                              @RequestHeader(value = "X-Admin-Id", required = false) String adminIdHeader) {
        return Result.ok(supervisionEventService.detail(id, headerId(userIdHeader), headerId(adminIdHeader)));
    }

    /** 管理员全部事件（分页）：GET /api/supervision/admin/list */
    @GetMapping("/admin/list")
    public Result<PageResult<SupervisionEventVO>> adminList(@RequestHeader(value = "X-Admin-Id", required = false) String adminIdHeader,
                                                            @RequestParam(defaultValue = "1") int page,
                                                            @RequestParam(defaultValue = "10") int size,
                                                            @RequestParam(required = false) String status,
                                                            @RequestParam(required = false) String keyword) {
        return Result.ok(supervisionEventService.pageAll(headerId(adminIdHeader), page, size, status, keyword));
    }

    /** 审核通过（管理员）：PUT /api/supervision/{id}/approve */
    @PutMapping("/{id}/approve")
    public Result<SupervisionEvent> approve(@PathVariable Long id,
                                            @RequestHeader(value = "X-Admin-Id", required = false) String adminIdHeader,
                                            @RequestBody(required = false) SupervisionReviewDTO body,
                                            @RequestParam(required = false) String remark) {
        String r = body != null ? body.getRemark() : null;
        if (!StringUtils.hasText(r)) r = remark;
        return Result.ok("审核通过", supervisionEventService.approve(id, headerId(adminIdHeader), r));
    }

    /** 管理员工作台统计：GET /api/supervision/admin/stats */
    @GetMapping("/admin/stats")
    public Result<WorkbenchStatsDTO> adminStats(@RequestHeader(value = "X-Admin-Id", required = false) String adminIdHeader) {
        return Result.ok(supervisionEventService.adminStats(headerId(adminIdHeader)));
    }

    /** 管理员待处理事件列表：GET /api/supervision/admin/pending */
    @GetMapping("/admin/pending")
    public Result<List<SupervisionEventVO>> adminPending(@RequestHeader(value = "X-Admin-Id", required = false) String adminIdHeader,
                                                         @RequestParam(defaultValue = "10") int limit) {
        return Result.ok(supervisionEventService.pendingEvents(headerId(adminIdHeader), limit));
    }

    /** 派单（管理员）：PUT /api/supervision/{id}/assign（APPROVED -> ASSIGNED，创建巡检任务） */
    @PutMapping("/{id}/assign")
    public Result<SupervisionEvent> assign(@PathVariable Long id,
                                           @RequestHeader(value = "X-Admin-Id", required = false) String adminIdHeader,
                                           @RequestBody(required = false) AssignEventDTO body) {
        return Result.ok("派单成功", supervisionEventService.assign(id, headerId(adminIdHeader), body));
    }

    /** 驳回（管理员）：PUT /api/supervision/{id}/reject */
    @PutMapping("/{id}/reject")
    public Result<SupervisionEvent> reject(@PathVariable Long id,
                                           @RequestHeader(value = "X-Admin-Id", required = false) String adminIdHeader,
                                           @RequestBody(required = false) SupervisionReviewDTO body,
                                           @RequestParam(required = false) String remark) {
        String r = body != null ? body.getRemark() : null;
        if (!StringUtils.hasText(r)) r = remark;
        return Result.ok("已驳回", supervisionEventService.reject(id, headerId(adminIdHeader), r));
    }
}