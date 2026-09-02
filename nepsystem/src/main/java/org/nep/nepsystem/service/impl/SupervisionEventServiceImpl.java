package org.nep.nepsystem.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.nep.nepsystem.bean.Admins;
import org.nep.nepsystem.bean.Devices;
import org.nep.nepsystem.bean.EventStatusLog;
import org.nep.nepsystem.bean.Regions;
import org.nep.nepsystem.bean.SupervisionAttachment;
import org.nep.nepsystem.bean.SupervisionEvent;
import org.nep.nepsystem.bean.InspectionTask;
import org.nep.nepsystem.bean.Users;
import org.nep.nepsystem.common.PageResult;
import org.nep.nepsystem.dao.AdminsDao;
import org.nep.nepsystem.dao.DevicesDao;
import org.nep.nepsystem.dao.EventStatusLogDao;
import org.nep.nepsystem.dao.RegionsDao;
import org.nep.nepsystem.dao.SupervisionAttachmentDao;
import org.nep.nepsystem.dao.SupervisionEventDao;
import org.nep.nepsystem.dao.InspectionTaskDao;
import org.nep.nepsystem.dao.UsersDao;
import org.nep.nepsystem.dto.SupervisionAttachmentDTO;
import org.nep.nepsystem.dto.AssignEventDTO;
import org.nep.nepsystem.dto.SupervisionCreateDTO;
import org.nep.nepsystem.dto.SupervisionDetailVO;
import org.nep.nepsystem.dto.WorkbenchStatsDTO;
import org.nep.nepsystem.dto.SupervisionEventVO;
import org.nep.nepsystem.dto.SupervisionStatusLogVO;
import org.nep.nepsystem.exception.BizException;
import org.nep.nepsystem.ws.NotifyWebSocketHandler;
import org.nep.nepsystem.service.SupervisionEventService;
import org.nep.nepsystem.dao.GridsDao;
import org.nep.nepsystem.bean.Grids;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

/**
 * 监督事件业务实现（Phase 2 新增）：
 * - 分层：Controller -> Service -> Mapper（Controller 不直接操作数据库）
 * - 状态机：创建=PENDING_REVIEW；审核通过=APPROVED；驳回=REJECTED；其余状态不在本 Phase 开放
 * - 每次状态变化（含创建）都写入 event_status_log
 * - 身份来源：公众 X-User-Id（users.id）、管理员 X-Admin-Id（admins.admin_id）
 *   （现有项目认证为轻量 token，正式认证体系接入后替换身份来源即可，接口不变）
 */
@Service
public class SupervisionEventServiceImpl extends ServiceImpl<SupervisionEventDao, SupervisionEvent> implements SupervisionEventService {

    @Autowired private UsersDao usersDao;
    @Autowired private AdminsDao adminsDao;
    @Autowired private DevicesDao devicesDao;
    @Autowired private RegionsDao regionsDao;
    @Autowired private SupervisionAttachmentDao supervisionAttachmentDao;
    @Autowired private EventStatusLogDao eventStatusLogDao;
    @Autowired private NotifyWebSocketHandler notifyWebSocketHandler;
    @Autowired private InspectionTaskDao inspectionTaskDao;
    @Autowired private GridsDao gridsDao;

    // ---------- 权限校验 ----------

    /** 校验公众身份：必须存在且启用，返回 userId */
    private Integer requireUser(Integer userId) {
        if (userId == null) {
            throw new BizException(401, "未登录或缺少用户身份(X-User-Id)");
        }
        Users user = usersDao.selectById(userId);
        if (user == null) {
            throw new BizException(401, "用户不存在: " + userId);
        }
        if (user.getStatus() == null || user.getStatus() != 1) {
            throw new BizException(403, "用户已被禁用");
        }
        return userId;
    }

    /** 校验管理员身份：必须存在，返回 adminId */
    private Integer requireAdmin(Integer adminId) {
        if (adminId == null) {
            throw new BizException(401, "未登录或缺少管理员身份(X-Admin-Id)");
        }
        Admins admin = adminsDao.selectById(adminId);
        if (admin == null) {
            throw new BizException(403, "管理员不存在或无权限");
        }
        return adminId;
    }

    /** 管理员身份是否有效（供详情接口放行判断，不抛异常） */
    private boolean isAdmin(Integer adminId) {
        return adminId != null && adminsDao.selectById(adminId) != null;
    }

    // ---------- 事件编号 ----------

    private String generateEventNo() {
        return "EV" + new SimpleDateFormat("yyyyMMddHHmmss").format(new Date())
                + String.format("%04d", ThreadLocalRandom.current().nextInt(10000));
    }

    // ---------- 状态日志 ----------

    /**
     * 写状态流转日志。
     * @param operatorId 操作人 users.id；管理员操作时置 NULL（外键指向 users，管理员账号记录在 remark）
     */
    private void insertStatusLog(Long eventId, String fromStatus, String toStatus, Integer operatorId, String operatorInfo, String remark) {
        EventStatusLog log = new EventStatusLog();
        log.setEventId(eventId);
        log.setFromStatus(fromStatus);
        log.setToStatus(toStatus);
        log.setOperatorId(operatorId);
        String fullRemark = remark;
        if (StringUtils.hasText(operatorInfo)) {
            fullRemark = operatorInfo + (StringUtils.hasText(remark) ? ": " + remark : "");
        }
        log.setRemark(fullRemark);
        log.setCreateTime(new Date());
        eventStatusLogDao.insert(log);
    }

    // ---------- VO 组装 ----------

    private SupervisionEventVO toVO(SupervisionEvent e) {
        SupervisionEventVO vo = new SupervisionEventVO();
        vo.setId(e.getId());
        vo.setEventNo(e.getEventNo());
        vo.setEventType(e.getEventType());
        vo.setTitle(e.getTitle());
        vo.setDescription(e.getDescription());
        vo.setDeviceId(e.getDeviceId());
        vo.setRegionId(e.getRegionId());
        vo.setLocation(e.getLocation());
        vo.setLat(e.getLat());
        vo.setLng(e.getLng());
        vo.setLevel(e.getLevel());
        vo.setStatus(e.getStatus());
        vo.setSubmitterId(e.getUserId());
        vo.setCreateTime(e.getCreateTime());
        vo.setUpdateTime(e.getUpdateTime());
        if (e.getDeviceId() != null) {
            Devices d = devicesDao.selectById(e.getDeviceId());
            if (d != null) vo.setDeviceName(d.getDeviceName());
        }
        if (e.getRegionId() != null) {
            Regions r = regionsDao.selectById(e.getRegionId());
            if (r != null) vo.setRegionName(r.getName());
        }
        if (e.getUserId() != null) {
            Users u = usersDao.selectById(e.getUserId());
            if (u != null) vo.setSubmitterName(StringUtils.hasText(u.getNickname()) ? u.getNickname() : u.getUsername());
        }
        return vo;
    }

    private SupervisionStatusLogVO toLogVO(EventStatusLog l) {
        SupervisionStatusLogVO vo = new SupervisionStatusLogVO();
        vo.setId(l.getId());
        vo.setEventId(l.getEventId());
        vo.setFromStatus(l.getFromStatus());
        vo.setToStatus(l.getToStatus());
        vo.setOperatorId(l.getOperatorId());
        vo.setRemark(l.getRemark());
        vo.setCreateTime(l.getCreateTime());
        // 操作人名称：管理员操作 operatorId 为 NULL（备注含管理员账号），否则查 users
        if (l.getOperatorId() != null) {
            Users u = usersDao.selectById(l.getOperatorId());
            if (u != null) vo.setOperatorName(StringUtils.hasText(u.getNickname()) ? u.getNickname() : u.getUsername());
        } else {
            vo.setOperatorName("系统/管理员");
        }
        return vo;
    }

    // ---------- 创建 ----------

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SupervisionEvent create(Integer userId, SupervisionCreateDTO dto) {
        requireUser(userId);
        if (dto == null) {
            throw new BizException(400, "请求体不能为空");
        }
        if (!StringUtils.hasText(dto.getEventType())) {
            throw new BizException(400, "事件类型不能为空");
        }
        if (!StringUtils.hasText(dto.getTitle())) {
            throw new BizException(400, "事件标题不能为空");
        }
        // 事件类型合法性校验（白名单，防止任意值）
        if (!TYPE_POLLUTION.equals(dto.getEventType()) && !TYPE_NOISE.equals(dto.getEventType())
                && !TYPE_DEVICE_FAULT.equals(dto.getEventType()) && !TYPE_OTHER.equals(dto.getEventType())) {
            throw new BizException(400, "非法事件类型: " + dto.getEventType());
        }
        if (dto.getDeviceId() != null && devicesDao.selectById(dto.getDeviceId()) == null) {
            throw new BizException(400, "关联设备不存在: " + dto.getDeviceId());
        }
        if (dto.getRegionId() != null && regionsDao.selectById(dto.getRegionId()) == null) {
            throw new BizException(400, "关联区域不存在: " + dto.getRegionId());
        }

        // 事件：状态由 Service 强制为 PENDING_REVIEW，不接受前端传入
        SupervisionEvent e = new SupervisionEvent();
        e.setEventNo(generateEventNo());
        e.setUserId(userId);
        e.setEventType(dto.getEventType());
        e.setTitle(dto.getTitle());
        e.setDescription(dto.getDescription());
        e.setDeviceId(dto.getDeviceId());
        e.setRegionId(dto.getRegionId());
        e.setLocation(dto.getLocation());
        e.setLat(dto.getLat());
        e.setLng(dto.getLng());
        e.setLevel(StringUtils.hasText(dto.getLevel()) ? dto.getLevel() : "WARN");
        e.setStatus(STATUS_PENDING_REVIEW);
        e.setCreateTime(new Date());
        e.setUpdateTime(new Date());
        super.save(e);

        // 附件登记
        if (dto.getAttachments() != null) {
            for (SupervisionAttachmentDTO a : dto.getAttachments()) {
                if (a == null || !StringUtils.hasText(a.getFileName())) {
                    continue;
                }
                SupervisionAttachment att = new SupervisionAttachment();
                att.setEventId(e.getId());
                att.setFileName(a.getFileName());
                att.setFilePath(a.getFilePath());
                att.setFileSize(a.getFileSize());
                att.setContentType(a.getContentType());
                att.setUploadUserId(userId);
                att.setCreateTime(new Date());
                supervisionAttachmentDao.insert(att);
            }
        }

        // 创建即状态变化：NULL -> PENDING_REVIEW
        insertStatusLog(e.getId(), null, STATUS_PENDING_REVIEW, userId, null, "提交监督事件");
        // Phase 7：实时通知管理员（仅提醒，数据以库为准）
        java.util.Map<String, Object> extra = new java.util.HashMap<>();
        extra.put("eventId", e.getId().toString());
        extra.put("eventNo", e.getEventNo());
        extra.put("title", e.getTitle());
        notifyWebSocketHandler.sendNotifyToAdmins("SUPERVISION_CREATED",
                "新的监督事件待审核：" + e.getEventNo() + " " + e.getTitle(), extra);
        return e;
    }

    // ---------- 我的事件（公众） ----------

    @Override
    public PageResult<SupervisionEventVO> pageMine(Integer userId, int page, int size, String status) {
        requireUser(userId);
        QueryWrapper<SupervisionEvent> qw = new QueryWrapper<SupervisionEvent>().eq("user_id", userId);
        if (StringUtils.hasText(status)) {
            qw.eq("status", status);
        }
        qw.orderByDesc("create_time");
        IPage<SupervisionEvent> p = this.page(new Page<>(page, size), qw);
        PageResult<SupervisionEventVO> pr = new PageResult<>();
        pr.setTotal(p.getTotal());
        pr.setPages(p.getPages());
        pr.setRecords(p.getRecords().stream().map(this::toVO).collect(Collectors.toList()));
        return pr;
    }

    // ---------- 详情（公众限本人 / 管理员全部） ----------

    @Override
    public SupervisionDetailVO detail(Long id, Integer userId, Integer adminId) {
        if (id == null) {
            throw new BizException(400, "缺少事件ID");
        }
        SupervisionEvent e = super.getById(id);
        if (e == null) {
            throw new BizException(400, "事件不存在: " + id);
        }
        // 权限：管理员（有效）放行；否则必须为事件本人
        if (!isAdmin(adminId)) {
            requireUser(userId);
            if (e.getUserId() == null || !e.getUserId().equals(userId)) {
                throw new BizException(403, "无权查看他人事件");
            }
        }
        SupervisionDetailVO vo = new SupervisionDetailVO();
        vo.setEvent(toVO(e));
        List<SupervisionAttachmentDTO> attachments = supervisionAttachmentDao
                .selectList(new QueryWrapper<SupervisionAttachment>().eq("event_id", id).orderByAsc("id"))
                .stream().map(a -> {
                    SupervisionAttachmentDTO dto = new SupervisionAttachmentDTO();
                    dto.setId(a.getId());
                    dto.setEventId(a.getEventId());
                    dto.setFileName(a.getFileName());
                    dto.setFilePath(a.getFilePath());
                    dto.setFileSize(a.getFileSize());
                    dto.setContentType(a.getContentType());
                    dto.setCreateTime(a.getCreateTime());
                    return dto;
                }).collect(Collectors.toList());
        vo.setAttachments(attachments);
        List<SupervisionStatusLogVO> logs = eventStatusLogDao
                .selectList(new QueryWrapper<EventStatusLog>().eq("event_id", id).orderByAsc("id"))
                .stream().map(this::toLogVO).collect(Collectors.toList());
        vo.setStatusLogs(logs);
        // Phase 6：关联巡检任务（未派单时为 null）
        InspectionTask linked = inspectionTaskDao.selectOne(
                new QueryWrapper<InspectionTask>().eq("event_id", e.getId()).orderByDesc("id").last("limit 1"));
        if (linked != null) {
            vo.setTaskId(linked.getId());
            vo.setTaskNo(linked.getTaskNo());
        }
        return vo;
    }

    // ---------- 管理员全部事件 ----------

    @Override
    public PageResult<SupervisionEventVO> pageAll(Integer adminId, int page, int size, String status, String keyword) {
        requireAdmin(adminId);
        QueryWrapper<SupervisionEvent> qw = new QueryWrapper<>();
        if (StringUtils.hasText(status)) {
            qw.eq("status", status);
        }
        if (StringUtils.hasText(keyword)) {
            qw.and(w -> w.like("event_no", keyword).or().like("title", keyword).or().like("description", keyword));
        }
        qw.orderByDesc("create_time");
        IPage<SupervisionEvent> p = this.page(new Page<>(page, size), qw);
        PageResult<SupervisionEventVO> pr = new PageResult<>();
        pr.setTotal(p.getTotal());
        pr.setPages(p.getPages());
        pr.setRecords(p.getRecords().stream().map(this::toVO).collect(Collectors.toList()));
        return pr;
    }

    // ---------- 审核（状态机在 Service 控制） ----------

    /** 公共审核前置：管理员身份 + 事件存在 + 必须处于 PENDING_REVIEW */
    private SupervisionEvent requireReviewable(Long id, Integer adminId) {
        requireAdmin(adminId);
        if (id == null) {
            throw new BizException(400, "缺少事件ID");
        }
        SupervisionEvent e = super.getById(id);
        if (e == null) {
            throw new BizException(400, "事件不存在: " + id);
        }
        if (!STATUS_PENDING_REVIEW.equals(e.getStatus())) {
            throw new BizException(400, "当前状态(" + e.getStatus() + ")不允许审核，仅待审核(PENDING_REVIEW)状态可审核");
        }
        return e;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SupervisionEvent approve(Long id, Integer adminId, String remark) {
        SupervisionEvent e = requireReviewable(id, adminId);
        Admins admin = adminsDao.selectById(adminId);
        String from = e.getStatus();
        e.setStatus(STATUS_APPROVED);
        e.setUpdateTime(new Date());
        super.updateById(e);
        // 管理员操作日志：operator_id 置 NULL（外键指向 users），管理员账号写入 remark
        insertStatusLog(e.getId(), from, STATUS_APPROVED, null,
                "管理员(" + (admin != null ? admin.getAdminCode() : adminId) + ")",
                StringUtils.hasText(remark) ? "审核通过: " + remark : "审核通过");
        return e;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SupervisionEvent reject(Long id, Integer adminId, String remark) {
        SupervisionEvent e = requireReviewable(id, adminId);
        Admins admin = adminsDao.selectById(adminId);
        String from = e.getStatus();
        e.setStatus(STATUS_REJECTED);
        e.setUpdateTime(new Date());
        super.updateById(e);
        insertStatusLog(e.getId(), from, STATUS_REJECTED, null,
                "管理员(" + (admin != null ? admin.getAdminCode() : adminId) + ")",
                StringUtils.hasText(remark) ? "驳回: " + remark : "驳回");
        return e;
    }

    // ---------- 派单（Phase 4 新增）：APPROVED -> ASSIGNED ----------

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SupervisionEvent assign(Long id, Integer adminId, AssignEventDTO dto) {
        requireAdmin(adminId);
        if (id == null) {
            throw new BizException(400, "缺少事件ID");
        }
        SupervisionEvent e = super.getById(id);
        if (e == null) {
            throw new BizException(400, "事件不存在: " + id);
        }
        if (!STATUS_APPROVED.equals(e.getStatus())) {
            throw new BizException(400, "当前状态(" + e.getStatus() + ")不允许派单，仅审核通过(APPROVED)状态可派单");
        }
        if (dto == null) {
            throw new BizException(400, "派单信息不能为空");
        }
        if (dto.getAssigneeId() == null) {
            throw new BizException(400, "请选择执行网格员");
        }
        // 网格校验（提供时）
        if (dto.getGridId() != null) {
            Grids grid = gridsDao.selectById(dto.getGridId());
            if (grid == null || (grid.getStatus() != null && grid.getStatus() != 1)) {
                throw new BizException(400, "网格不存在或已停用: " + dto.getGridId());
            }
        }
        // 网格员校验
        Users assignee = usersDao.selectById(dto.getAssigneeId());
        if (assignee == null || assignee.getStatus() == null || assignee.getStatus() != 1) {
            throw new BizException(400, "网格员不存在或已禁用: " + dto.getAssigneeId());
        }
        // 优先级白名单
        String priority = dto.getPriority();
        if (!StringUtils.hasText(priority)) {
            priority = "MEDIUM";
        }
        if (!"LOW".equals(priority) && !"MEDIUM".equals(priority) && !"HIGH".equals(priority)) {
            throw new BizException(400, "非法优先级: " + priority);
        }

        Admins admin = adminsDao.selectById(adminId);
        String from = e.getStatus();
        e.setStatus(STATUS_ASSIGNED);
        e.setAssigneeId(dto.getAssigneeId());
        e.setUpdateTime(new Date());
        super.updateById(e);
        // 状态日志
        insertStatusLog(e.getId(), from, STATUS_ASSIGNED, null,
                "管理员(" + (admin != null ? admin.getAdminCode() : adminId) + ")",
                StringUtils.hasText(dto.getRemark()) ? "派单: " + dto.getRemark() : "派单");
        // 创建关联巡检任务（status=ASSIGNED 直接进入已指派）
        InspectionTask task = new InspectionTask();
        task.setTaskNo("TK" + new SimpleDateFormat("yyyyMMddHHmmss").format(new Date())
                + String.format("%04d", ThreadLocalRandom.current().nextInt(10000)));
        task.setEventId(e.getId());
        task.setDeviceId(e.getDeviceId());
        task.setGridId(dto.getGridId());
        task.setAssigneeId(dto.getAssigneeId());
        task.setTaskType("INSPECTION");
        task.setPriority(priority);
        task.setStatus(STATUS_ASSIGNED);
        task.setDeadline(dto.getDeadline());
        task.setResult(StringUtils.hasText(dto.getRemark()) ? dto.getRemark() : null);
        task.setCreateTime(new Date());
        task.setUpdateTime(new Date());
        inspectionTaskDao.insert(task);
        // Phase 7：实时通知网格员（仅提醒，数据以库为准）
        java.util.Map<String, Object> extra = new java.util.HashMap<>();
        extra.put("taskId", task.getId().toString());
        extra.put("taskNo", task.getTaskNo());
        extra.put("eventId", e.getId().toString());
        extra.put("eventNo", e.getEventNo());
        extra.put("priority", priority);
        notifyWebSocketHandler.sendNotifyToUser(dto.getAssigneeId(), "TASK_ASSIGNED",
                "您有新任务：" + task.getTaskNo() + "（" + e.getTitle() + "）", extra);
        return e;
    }

    // ---------- 工作台统计（Phase 4 新增） ----------

    @Override
    public WorkbenchStatsDTO adminStats(Integer adminId) {
        requireAdmin(adminId);
        WorkbenchStatsDTO dto = new WorkbenchStatsDTO();
        java.util.Calendar cal = java.util.Calendar.getInstance();
        cal.set(java.util.Calendar.HOUR_OF_DAY, 0);
        cal.set(java.util.Calendar.MINUTE, 0);
        cal.set(java.util.Calendar.SECOND, 0);
        cal.set(java.util.Calendar.MILLISECOND, 0);
        Date today = cal.getTime();
        dto.setTodayEvents(Long.valueOf(super.count(new QueryWrapper<SupervisionEvent>().ge("create_time", today))));
        dto.setPendingReview(Long.valueOf(super.count(new QueryWrapper<SupervisionEvent>().eq("status", STATUS_PENDING_REVIEW))));
        dto.setPendingAssign(Long.valueOf(super.count(new QueryWrapper<SupervisionEvent>().eq("status", STATUS_APPROVED))));
        dto.setProcessing(Long.valueOf(super.count(new QueryWrapper<SupervisionEvent>().in("status",
                STATUS_ASSIGNED, "ACCEPTED", "INSPECTING", "INSPECTED", "VERIFIED"))));
        dto.setTodayClosed(Long.valueOf(super.count(new QueryWrapper<SupervisionEvent>()
                .eq("status", STATUS_CLOSED).ge("update_time", today))));
        return dto;
    }

    // ---------- 待处理事件（Phase 4 新增） ----------

    @Override
    public java.util.List<SupervisionEventVO> pendingEvents(Integer adminId, int limit) {
        requireAdmin(adminId);
        int n = Math.min(Math.max(limit, 1), 100);
        List<SupervisionEvent> list = super.list(new QueryWrapper<SupervisionEvent>()
                .in("status", STATUS_PENDING_REVIEW, STATUS_APPROVED, STATUS_ASSIGNED,
                        "ACCEPTED", "INSPECTING", "INSPECTED", "VERIFIED")
                .orderByDesc("create_time")
                .last("limit " + n));
        return list.stream().map(this::toVO).collect(java.util.stream.Collectors.toList());
    }
}