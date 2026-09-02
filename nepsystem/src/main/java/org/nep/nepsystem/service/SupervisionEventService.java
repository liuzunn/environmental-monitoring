package org.nep.nepsystem.service;

import org.nep.nepsystem.bean.SupervisionEvent;
import org.nep.nepsystem.common.PageResult;
import org.nep.nepsystem.dto.SupervisionCreateDTO;
import org.nep.nepsystem.dto.SupervisionDetailVO;
import org.nep.nepsystem.dto.SupervisionEventVO;

/**
 * 监督事件业务（Phase 2 新增）：
 * 状态机由 Service 控制（前端不可直接传 status），所有状态变化写入 event_status_log。
 * 权限：公众只能操作自己的事件；管理员可查看全部并可审核。
 */
public interface SupervisionEventService {

    /** 事件状态常量 */
    String STATUS_PENDING_REVIEW = "PENDING_REVIEW";
    String STATUS_APPROVED = "APPROVED";
    String STATUS_REJECTED = "REJECTED";
    String STATUS_ASSIGNED = "ASSIGNED";
    String STATUS_CLOSED = "CLOSED";

    /** 事件类型常量 */
    String TYPE_POLLUTION = "POLLUTION";
    String TYPE_NOISE = "NOISE";
    String TYPE_DEVICE_FAULT = "DEVICE_FAULT";
    String TYPE_OTHER = "OTHER";

    /**
     * 创建监督事件（公众）：状态置 PENDING_REVIEW，写入附件登记与状态日志。
     * @param userId 提交人ID（请求头 X-User-Id 解析，需为有效启用用户）
     */
    SupervisionEvent create(Integer userId, SupervisionCreateDTO dto);

    /** 查询自己的监督事件（公众，分页） */
    PageResult<SupervisionEventVO> pageMine(Integer userId, int page, int size, String status);

    /** 查询监督详情：公众仅限本人事件；管理员可查看全部 */
    SupervisionDetailVO detail(Long id, Integer userId, Integer adminId);

    /** 管理员派单（Phase 4 新增）：APPROVED -> ASSIGNED，创建关联巡检任务并写状态日志 */
    SupervisionEvent assign(Long id, Integer adminId, org.nep.nepsystem.dto.AssignEventDTO dto);

    /** 管理员工作台统计（Phase 4 新增） */
    org.nep.nepsystem.dto.WorkbenchStatsDTO adminStats(Integer adminId);

    /** 管理员待处理事件列表（Phase 4 新增）：所有未关闭状态，按时间倒序 */
    java.util.List<org.nep.nepsystem.dto.SupervisionEventVO> pendingEvents(Integer adminId, int limit);

    /** 管理员查询全部事件（分页，支持状态/关键字过滤） */
    PageResult<SupervisionEventVO> pageAll(Integer adminId, int page, int size, String status, String keyword);

    /** 管理员审核通过：PENDING_REVIEW -> APPROVED（写状态日志） */
    SupervisionEvent approve(Long id, Integer adminId, String remark);

    /** 管理员驳回：PENDING_REVIEW -> REJECTED（写状态日志） */
    SupervisionEvent reject(Long id, Integer adminId, String remark);
}