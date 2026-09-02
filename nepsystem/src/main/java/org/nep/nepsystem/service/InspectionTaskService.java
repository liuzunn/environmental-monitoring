package org.nep.nepsystem.service;

import org.nep.nepsystem.bean.InspectionTask;
import org.nep.nepsystem.common.PageResult;
import org.nep.nepsystem.dto.AssigneeStatsVO;
import org.nep.nepsystem.dto.InspectionTaskCreateDTO;
import org.nep.nepsystem.dto.TaskVO;

import java.util.List;

/**
 * 巡检任务业务（Phase 4 新增，管理员侧）：
 * 创建任务（网格/网格员/优先级/截止时间）、任务列表、网格员完成率统计
 */
public interface InspectionTaskService {

    /** 创建任务（管理员，直接进入 ASSIGNED 已指派） */
    InspectionTask create(Integer adminId, InspectionTaskCreateDTO dto);

    /** 任务分页（status/assigneeId/gridId/keyword 过滤） */
    PageResult<TaskVO> page(Integer adminId, int page, int size, String status,
                            Integer assigneeId, Integer gridId, String keyword);

    /** 网格员任务完成率统计（CLOSED 计完成） */
    List<AssigneeStatsVO> assigneeStats(Integer adminId);

    // ===== Phase 5 网格员端（身份 X-User-Id） =====

    /** 我的任务（网格员）：仅本人任务，支持状态/关键字过滤 */
    PageResult<TaskVO> pageMine(Integer userId, int page, int size, String status, String keyword);

    /** 我的任务统计（网格员）：待接收/进行中/今日完成/超时 */
    org.nep.nepsystem.dto.TaskStatsVO mineStats(Integer userId);

    /** 任务详情（网格员）：仅本人任务，含公众反馈（关联事件+附件） */
    org.nep.nepsystem.dto.TaskDetailVO detailMine(Long id, Integer userId);

    /** 接收任务：ASSIGNED -> ACCEPTED（事件同步） */
    InspectionTask accept(Long id, Integer userId);

    /** 开始检测：ACCEPTED -> INSPECTING（事件同步） */
    InspectionTask start(Long id, Integer userId);

    /** 提交检测：INSPECTING -> INSPECTED，写 inspection_record（六项+AQI+照片+备注+坐标，事件同步） */
    InspectionTask submit(Long id, Integer userId, org.nep.nepsystem.dto.DetectSubmitDTO dto);

    // ===== Phase 6 闭环（管理员） =====

    /** 查看任务检测记录（管理员） */
    java.util.List<org.nep.nepsystem.dto.InspectionRecordVO> records(Integer adminId, Long taskId);

    /** 核实检测结果（管理员）：任务 INSPECTED -> VERIFIED（事件同步+日志） */
    InspectionTask verify(Integer adminId, Long taskId, String remark);

    /** 关闭任务/事件（管理员）：任务 VERIFIED -> CLOSED（事件同步+日志） */
    InspectionTask close(Integer adminId, Long taskId, String remark);
}