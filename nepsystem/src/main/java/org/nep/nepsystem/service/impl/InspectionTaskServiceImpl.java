package org.nep.nepsystem.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.nep.nepsystem.bean.Devices;
import org.nep.nepsystem.bean.Grids;
import org.nep.nepsystem.bean.InspectionTask;
import org.nep.nepsystem.bean.SupervisionEvent;
import org.nep.nepsystem.bean.Users;
import org.nep.nepsystem.common.PageResult;
import org.nep.nepsystem.dao.AdminsDao;
import org.nep.nepsystem.dao.DevicesDao;
import org.nep.nepsystem.dao.GridsDao;
import org.nep.nepsystem.dao.InspectionTaskDao;
import org.nep.nepsystem.dao.SupervisionEventDao;
import org.nep.nepsystem.dao.InspectionRecordDao;
import org.nep.nepsystem.dao.SupervisionAttachmentDao;
import org.nep.nepsystem.dao.EventStatusLogDao;
import org.nep.nepsystem.bean.InspectionRecord;
import org.nep.nepsystem.bean.SupervisionAttachment;
import org.nep.nepsystem.bean.EventStatusLog;
import org.nep.nepsystem.service.SupervisionEventService;
import org.nep.nepsystem.dao.UsersDao;
import org.nep.nepsystem.dto.AssigneeStatsVO;
import org.nep.nepsystem.dto.InspectionTaskCreateDTO;
import org.nep.nepsystem.dto.TaskVO;
import org.nep.nepsystem.dto.TaskDetailVO;
import org.nep.nepsystem.dto.TaskStatsVO;
import org.nep.nepsystem.dto.DetectSubmitDTO;
import org.nep.nepsystem.dto.SupervisionEventVO;
import org.nep.nepsystem.dto.SupervisionAttachmentDTO;
import org.nep.nepsystem.dto.InspectionRecordVO;
import org.nep.nepsystem.exception.BizException;
import org.nep.nepsystem.ws.NotifyWebSocketHandler;
import org.nep.nepsystem.service.InspectionTaskService;
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
 * 巡检任务业务实现（Phase 4 新增，管理员侧）
 */
@Service
public class InspectionTaskServiceImpl extends ServiceImpl<InspectionTaskDao, InspectionTask> implements InspectionTaskService {

    @Autowired private AdminsDao adminsDao;
    @Autowired private GridsDao gridsDao;
    @Autowired private UsersDao usersDao;
    @Autowired private DevicesDao devicesDao;
    @Autowired private SupervisionEventDao supervisionEventDao;
    @Autowired private InspectionRecordDao inspectionRecordDao;
    @Autowired private SupervisionAttachmentDao supervisionAttachmentDao;
    @Autowired private EventStatusLogDao eventStatusLogDao;
    @Autowired private NotifyWebSocketHandler notifyWebSocketHandler;

    private void requireAdmin(Integer adminId) {
        if (adminId == null || adminsDao.selectById(adminId) == null) {
            throw new BizException(403, "管理员不存在或无权限");
        }
    }

    private TaskVO toVO(InspectionTask t) {
        TaskVO vo = new TaskVO();
        vo.setId(t.getId());
        vo.setTaskNo(t.getTaskNo());
        vo.setEventId(t.getEventId());
        vo.setDeviceId(t.getDeviceId());
        vo.setGridId(t.getGridId());
        vo.setAssigneeId(t.getAssigneeId());
        vo.setTaskType(t.getTaskType());
        vo.setPriority(t.getPriority());
        vo.setStatus(t.getStatus());
        vo.setDeadline(t.getDeadline());
        vo.setResult(t.getResult());
        vo.setCreateTime(t.getCreateTime());
        vo.setUpdateTime(t.getUpdateTime());
        if (t.getEventId() != null) {
            SupervisionEvent e = supervisionEventDao.selectById(t.getEventId());
            if (e != null) vo.setEventTitle(e.getTitle());
        }
        if (t.getGridId() != null) {
            Grids g = gridsDao.selectById(t.getGridId());
            if (g != null) vo.setGridName(g.getGridName());
        }
        if (t.getDeviceId() != null) {
            Devices d = devicesDao.selectById(t.getDeviceId());
            if (d != null) vo.setDeviceName(d.getDeviceName());
        }
        if (t.getAssigneeId() != null) {
            Users u = usersDao.selectById(t.getAssigneeId());
            if (u != null) vo.setAssigneeName(StringUtils.hasText(u.getNickname()) ? u.getNickname() : u.getUsername());
        }
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public InspectionTask create(Integer adminId, InspectionTaskCreateDTO dto) {
        requireAdmin(adminId);
        if (dto == null) {
            throw new BizException(400, "任务信息不能为空");
        }
        if (dto.getAssigneeId() == null) {
            throw new BizException(400, "请选择执行网格员");
        }
        Users assignee = usersDao.selectById(dto.getAssigneeId());
        if (assignee == null || assignee.getStatus() == null || assignee.getStatus() != 1) {
            throw new BizException(400, "网格员不存在或已禁用: " + dto.getAssigneeId());
        }
        if (dto.getGridId() != null) {
            Grids grid = gridsDao.selectById(dto.getGridId());
            if (grid == null || (grid.getStatus() != null && grid.getStatus() != 1)) {
                throw new BizException(400, "网格不存在或已停用: " + dto.getGridId());
            }
        }
        if (dto.getDeviceId() != null && devicesDao.selectById(dto.getDeviceId()) == null) {
            throw new BizException(400, "设备不存在: " + dto.getDeviceId());
        }
        if (dto.getEventId() != null && supervisionEventDao.selectById(dto.getEventId()) == null) {
            throw new BizException(400, "关联事件不存在: " + dto.getEventId());
        }
        String priority = StringUtils.hasText(dto.getPriority()) ? dto.getPriority() : "MEDIUM";
        if (!"LOW".equals(priority) && !"MEDIUM".equals(priority) && !"HIGH".equals(priority)) {
            throw new BizException(400, "非法优先级: " + priority);
        }
        String taskType = StringUtils.hasText(dto.getTaskType()) ? dto.getTaskType() : "INSPECTION";
        InspectionTask t = new InspectionTask();
        t.setTaskNo("TK" + new SimpleDateFormat("yyyyMMddHHmmss").format(new Date())
                + String.format("%04d", ThreadLocalRandom.current().nextInt(10000)));
        t.setEventId(dto.getEventId());
        t.setDeviceId(dto.getDeviceId());
        t.setGridId(dto.getGridId());
        t.setAssigneeId(dto.getAssigneeId());
        t.setTaskType(taskType);
        t.setPriority(priority);
        t.setStatus("ASSIGNED");
        t.setDeadline(dto.getDeadline());
        t.setResult(dto.getDescription());
        t.setCreateTime(new Date());
        t.setUpdateTime(new Date());
        super.save(t);
        return t;
    }

    @Override
    public PageResult<TaskVO> page(Integer adminId, int page, int size, String status,
                                   Integer assigneeId, Integer gridId, String keyword) {
        requireAdmin(adminId);
        QueryWrapper<InspectionTask> qw = new QueryWrapper<>();
        if (StringUtils.hasText(status)) qw.eq("status", status);
        if (assigneeId != null) qw.eq("assignee_id", assigneeId);
        if (gridId != null) qw.eq("grid_id", gridId);
        if (StringUtils.hasText(keyword)) {
            qw.and(w -> w.like("task_no", keyword).or().like("result", keyword));
        }
        qw.orderByDesc("create_time");
        IPage<InspectionTask> p = this.page(new Page<>(page, size), qw);
        PageResult<TaskVO> pr = new PageResult<>();
        pr.setTotal(p.getTotal());
        pr.setPages(p.getPages());
        pr.setRecords(p.getRecords().stream().map(this::toVO).collect(Collectors.toList()));
        return pr;
    }

    @Override
    public List<AssigneeStatsVO> assigneeStats(Integer adminId) {
        requireAdmin(adminId);
        // 按网格员分组统计
        List<Map<String, Object>> rows = this.listMaps(new QueryWrapper<InspectionTask>()
                .select("assignee_id", "COUNT(*) AS total",
                        "SUM(CASE WHEN status = 'CLOSED' THEN 1 ELSE 0 END) AS closed")
                .isNotNull("assignee_id")
                .groupBy("assignee_id"));
        List<AssigneeStatsVO> out = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            Object aid = row.get("assignee_id");
            if (aid == null) continue;
            Integer userId = ((Number) aid).intValue();
            long total = ((Number) row.get("total")).longValue();
            long closed = row.get("closed") == null ? 0 : ((Number) row.get("closed")).longValue();
            AssigneeStatsVO vo = new AssigneeStatsVO();
            vo.setUserId(userId);
            vo.setTotalTasks(total);
            vo.setClosedTasks(closed);
            vo.setCompletionRate(total == 0 ? 0 : (int) Math.round(closed * 100.0 / total));
            Users u = usersDao.selectById(userId);
            if (u != null) {
                vo.setUserName(StringUtils.hasText(u.getNickname()) ? u.getNickname() : u.getUsername());
            }
            out.add(vo);
        }
        out.sort((a, b) -> Long.compare(b.getTotalTasks(), a.getTotalTasks()));
        return out;
    }

    // ===== Phase 5 网格员端 =====

    private Integer requireUser(Integer userId) {
        if (userId == null) {
            throw new BizException(401, "未登录或缺少用户身份(X-User-Id)");
        }
        Users u = usersDao.selectById(userId);
        if (u == null || u.getStatus() == null || u.getStatus() != 1) {
            throw new BizException(403, "用户不存在或已禁用");
        }
        return userId;
    }

    /** 取本人任务并校验归属 */
    private InspectionTask requireOwnTask(Long id, Integer userId) {
        requireUser(userId);
        if (id == null) {
            throw new BizException(400, "缺少任务ID");
        }
        InspectionTask t = super.getById(id);
        if (t == null) {
            throw new BizException(400, "任务不存在: " + id);
        }
        if (t.getAssigneeId() == null || !t.getAssigneeId().equals(userId)) {
            throw new BizException(403, "无权操作他人任务");
        }
        return t;
    }

    /** 任务状态流转（含事件同步与状态日志） */
    private InspectionTask transit(Long id, Integer userId, String from, String to, String actionRemark) {
        InspectionTask t = requireOwnTask(id, userId);
        if (!from.equals(t.getStatus())) {
            throw new BizException(400, "当前状态(" + t.getStatus() + ")不允许该操作，需要状态 " + from);
        }
        t.setStatus(to);
        t.setUpdateTime(new Date());
        super.updateById(t);
        // 事件同步
        if (t.getEventId() != null) {
            SupervisionEvent e = supervisionEventDao.selectById(t.getEventId());
            if (e != null && from.equals(e.getStatus())) {
                e.setStatus(to);
                e.setUpdateTime(new Date());
                supervisionEventDao.updateById(e);
                EventStatusLog log = new EventStatusLog();
                log.setEventId(e.getId());
                log.setFromStatus(from);
                log.setToStatus(to);
                log.setOperatorId(userId);
                log.setRemark(actionRemark);
                log.setCreateTime(new Date());
                eventStatusLogDao.insert(log);
            }
        }
        return t;
    }

    @Override
    public PageResult<TaskVO> pageMine(Integer userId, int page, int size, String status, String keyword) {
        requireUser(userId);
        QueryWrapper<InspectionTask> qw = new QueryWrapper<InspectionTask>().eq("assignee_id", userId);
        if (StringUtils.hasText(status)) qw.eq("status", status);
        if (StringUtils.hasText(keyword)) {
            qw.and(w -> w.like("task_no", keyword).or().like("result", keyword));
        }
        qw.orderByDesc("create_time");
        IPage<InspectionTask> p = this.page(new Page<>(page, size), qw);
        PageResult<TaskVO> pr = new PageResult<>();
        pr.setTotal(p.getTotal());
        pr.setPages(p.getPages());
        pr.setRecords(p.getRecords().stream().map(this::toVO).collect(Collectors.toList()));
        return pr;
    }

    @Override
    public TaskStatsVO mineStats(Integer userId) {
        requireUser(userId);
        TaskStatsVO vo = new TaskStatsVO();
        java.util.Calendar cal = java.util.Calendar.getInstance();
        cal.set(java.util.Calendar.HOUR_OF_DAY, 0);
        cal.set(java.util.Calendar.MINUTE, 0);
        cal.set(java.util.Calendar.SECOND, 0);
        cal.set(java.util.Calendar.MILLISECOND, 0);
        Date today = cal.getTime();
        QueryWrapper<InspectionTask> base = new QueryWrapper<InspectionTask>().eq("assignee_id", userId);
        vo.setPendingAccept(Long.valueOf(this.count(new QueryWrapper<InspectionTask>()
                .eq("assignee_id", userId).eq("status", "ASSIGNED"))));
        vo.setProcessing(Long.valueOf(this.count(new QueryWrapper<InspectionTask>()
                .eq("assignee_id", userId).in("status", "ACCEPTED", "INSPECTING"))));
        vo.setTodayDone(Long.valueOf(this.count(new QueryWrapper<InspectionTask>()
                .eq("assignee_id", userId).eq("status", "INSPECTED").ge("update_time", today))));
        vo.setOverdue(Long.valueOf(this.count(new QueryWrapper<InspectionTask>()
                .eq("assignee_id", userId).isNotNull("deadline").lt("deadline", new Date())
                .notIn("status", "INSPECTED", "CLOSED"))));
        return vo;
    }

    private SupervisionEventVO toEventVO(SupervisionEvent e) {
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
        if (e.getUserId() != null) {
            Users u = usersDao.selectById(e.getUserId());
            if (u != null) vo.setSubmitterName(StringUtils.hasText(u.getNickname()) ? u.getNickname() : u.getUsername());
        }
        return vo;
    }

    @Override
    public TaskDetailVO detailMine(Long id, Integer userId) {
        InspectionTask t = requireOwnTask(id, userId);
        TaskDetailVO vo = new TaskDetailVO();
        vo.setTask(toVO(t));
        if (t.getEventId() != null) {
            SupervisionEvent e = supervisionEventDao.selectById(t.getEventId());
            if (e != null) {
                vo.setEvent(toEventVO(e));
                vo.setEventAttachments(supervisionAttachmentDao
                        .selectList(new QueryWrapper<SupervisionAttachment>().eq("event_id", e.getId()).orderByAsc("id"))
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
                        }).collect(Collectors.toList()));
            }
        }
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public InspectionTask accept(Long id, Integer userId) {
        InspectionTask t = transit(id, userId, "ASSIGNED", "ACCEPTED", "网格员接收任务");
        // Phase 7：实时通知管理员
        java.util.Map<String, Object> extra = new java.util.HashMap<>();
        extra.put("taskId", t.getId().toString());
        extra.put("taskNo", t.getTaskNo());
        extra.put("assigneeId", userId);
        notifyWebSocketHandler.sendNotifyToAdmins("TASK_ACCEPTED",
                "网格员已接单：" + t.getTaskNo(), extra);
        return t;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public InspectionTask start(Long id, Integer userId) {
        return transit(id, userId, "ACCEPTED", "INSPECTING", "网格员开始检测");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public InspectionTask submit(Long id, Integer userId, DetectSubmitDTO dto) {
        InspectionTask t = requireOwnTask(id, userId);
        if (!"INSPECTING".equals(t.getStatus())) {
            throw new BizException(400, "当前状态(" + t.getStatus() + ")不允许提交检测，需要状态 INSPECTING");
        }
        if (dto == null) {
            throw new BizException(400, "检测数据不能为空");
        }
        // 至少一项检测值
        if (dto.getPm25() == null && dto.getPm10() == null && dto.getSo2() == null
                && dto.getNo2() == null && dto.getCo() == null && dto.getO3() == null) {
            throw new BizException(400, "请至少填写一项污染物检测值");
        }
        Integer aqi = calcAQI(dto);
        // 写巡检记录
        InspectionRecord rec = new InspectionRecord();
        rec.setTaskId(t.getId());
        rec.setRecordType("INSPECT");
        rec.setContent(dto.getContent());
        rec.setPm25(dto.getPm25());
        rec.setPm10(dto.getPm10());
        rec.setSo2(dto.getSo2());
        rec.setNo2(dto.getNo2());
        rec.setCo(dto.getCo());
        rec.setO3(dto.getO3());
        rec.setAqiValue(aqi);
        rec.setLat(dto.getLat());
        rec.setLng(dto.getLng());
        rec.setImages(dto.getImages() == null || dto.getImages().isEmpty() ? null : String.join(",", dto.getImages()));
        rec.setRecorderId(userId);
        rec.setCreateTime(new Date());
        inspectionRecordDao.insert(rec);
        // 任务置 INSPECTED
        t.setStatus("INSPECTED");
        t.setResult(StringUtils.hasText(dto.getContent()) ? dto.getContent() : "现场检测完成，AQI=" + (aqi == null ? "-" : aqi));
        t.setUpdateTime(new Date());
        super.updateById(t);
        // Phase 7：实时通知管理员（检测完成）
        java.util.Map<String, Object> extra = new java.util.HashMap<>();
        extra.put("taskId", t.getId().toString());
        extra.put("taskNo", t.getTaskNo());
        extra.put("aqi", aqi == null ? null : aqi.toString());
        extra.put("eventId", t.getEventId() == null ? null : t.getEventId().toString());
        notifyWebSocketHandler.sendNotifyToAdmins("DETECT_SUBMITTED",
                "检测完成 AQI=" + (aqi == null ? "-" : aqi) + "：" + t.getTaskNo(), extra);
        // 事件同步
        if (t.getEventId() != null) {
            SupervisionEvent e = supervisionEventDao.selectById(t.getEventId());
            if (e != null && "INSPECTING".equals(e.getStatus())) {
                e.setStatus("INSPECTED");
                e.setUpdateTime(new Date());
                supervisionEventDao.updateById(e);
                EventStatusLog log = new EventStatusLog();
                log.setEventId(e.getId());
                log.setFromStatus("INSPECTING");
                log.setToStatus("INSPECTED");
                log.setOperatorId(userId);
                log.setRemark("网格员提交检测，AQI=" + (aqi == null ? "-" : aqi));
                log.setCreateTime(new Date());
                eventStatusLogDao.insert(log);
            }
        }
        return t;
    }

    // ===== Phase 6 闭环（管理员） =====

    @Override
    public java.util.List<InspectionRecordVO> records(Integer adminId, Long taskId) {
        requireAdmin(adminId);
        if (taskId == null || super.getById(taskId) == null) {
            throw new BizException(400, "任务不存在: " + taskId);
        }
        return inspectionRecordDao
                .selectList(new QueryWrapper<InspectionRecord>().eq("task_id", taskId).orderByAsc("id"))
                .stream().map(rec -> {
                    InspectionRecordVO vo = new InspectionRecordVO();
                    vo.setId(rec.getId());
                    vo.setTaskId(rec.getTaskId());
                    vo.setRecordType(rec.getRecordType());
                    vo.setContent(rec.getContent());
                    vo.setPm25(rec.getPm25());
                    vo.setPm10(rec.getPm10());
                    vo.setSo2(rec.getSo2());
                    vo.setNo2(rec.getNo2());
                    vo.setCo(rec.getCo());
                    vo.setO3(rec.getO3());
                    vo.setAqiValue(rec.getAqiValue());
                    vo.setImages(rec.getImages() == null ? null : java.util.Arrays.asList(rec.getImages().split(",")));
                    vo.setLat(rec.getLat());
                    vo.setLng(rec.getLng());
                    vo.setRecorderId(rec.getRecorderId());
                    if (rec.getRecorderId() != null) {
                        Users u = usersDao.selectById(rec.getRecorderId());
                        if (u != null) {
                            vo.setRecorderName(StringUtils.hasText(u.getNickname()) ? u.getNickname() : u.getUsername());
                        }
                    }
                    vo.setCreateTime(rec.getCreateTime());
                    return vo;
                }).collect(Collectors.toList());
    }

    /** 管理员核实/关闭通用流转（任务+事件同步+日志） */
    private InspectionTask adminTransit(Integer adminId, Long taskId, String from, String to, String actionRemark) {
        requireAdmin(adminId);
        if (taskId == null) {
            throw new BizException(400, "缺少任务ID");
        }
        InspectionTask t = super.getById(taskId);
        if (t == null) {
            throw new BizException(400, "任务不存在: " + taskId);
        }
        if (!from.equals(t.getStatus())) {
            throw new BizException(400, "当前状态(" + t.getStatus() + ")不允许该操作，需要状态 " + from);
        }
        t.setStatus(to);
        t.setUpdateTime(new Date());
        super.updateById(t);
        if (t.getEventId() != null) {
            SupervisionEvent e = supervisionEventDao.selectById(t.getEventId());
            if (e != null && from.equals(e.getStatus())) {
                e.setStatus(to);
                e.setUpdateTime(new Date());
                supervisionEventDao.updateById(e);
                EventStatusLog log = new EventStatusLog();
                log.setEventId(e.getId());
                log.setFromStatus(from);
                log.setToStatus(to);
                log.setOperatorId(null);
                log.setRemark(actionRemark);
                log.setCreateTime(new Date());
                eventStatusLogDao.insert(log);
            }
        }
        return t;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public InspectionTask verify(Integer adminId, Long taskId, String remark) {
        return adminTransit(adminId, taskId, "INSPECTED", "VERIFIED",
                StringUtils.hasText(remark) ? "管理员核实通过: " + remark : "管理员核实通过");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public InspectionTask close(Integer adminId, Long taskId, String remark) {
        InspectionTask t = adminTransit(adminId, taskId, "VERIFIED", "CLOSED",
                StringUtils.hasText(remark) ? "管理员关闭: " + remark : "管理员关闭事件");
        // Phase 7：实时通知公众（事件关闭）
        if (t.getEventId() != null) {
            SupervisionEvent e = supervisionEventDao.selectById(t.getEventId());
            if (e != null && e.getUserId() != null) {
                java.util.Map<String, Object> extra = new java.util.HashMap<>();
                extra.put("eventId", e.getId().toString());
                extra.put("eventNo", e.getEventNo());
                extra.put("title", e.getTitle());
                notifyWebSocketHandler.sendNotifyToUser(e.getUserId(), "EVENT_CLOSED",
                        "您的监督事件已处理完成：" + e.getEventNo(), extra);
            }
        }
        return t;
    }

    // ---------- AQI 计算（HJ633-2012，1小时均值分段表，缺项不参与） ----------

    /** 分段表：每项 [浓度下限, 浓度上限, IAQI下限, IAQI上限] */
    private static final double[][] PM25_BP = {{0,35,0,50},{35,75,50,100},{75,115,100,150},{115,150,150,200},{150,250,200,300},{250,350,300,400},{350,500,400,500}};
    private static final double[][] PM10_BP = {{0,50,0,50},{50,150,50,100},{150,250,100,150},{250,350,150,200},{350,420,200,300},{420,500,300,400},{500,600,400,500}};
    private static final double[][] SO2_BP  = {{0,50,0,50},{50,150,50,100},{150,475,100,150},{475,800,150,200},{800,1600,200,300},{1600,2100,300,400},{2100,2620,400,500}};
    private static final double[][] NO2_BP  = {{0,40,0,50},{40,80,50,100},{80,180,100,150},{180,280,150,200},{280,565,200,300},{565,750,300,400},{750,940,400,500}};
    private static final double[][] CO_BP   = {{0,5,0,50},{5,10,50,100},{10,35,100,150},{35,60,150,200},{60,90,200,300},{90,120,300,400},{120,150,400,500}};
    private static final double[][] O3_BP   = {{0,160,0,50},{160,200,50,100},{200,300,100,150},{300,400,150,200},{400,800,200,300},{800,1000,300,400},{1000,1200,400,500}};

    private static int iaqi(double c, double[][] bp) {
        if (c < 0) return 0;
        for (double[] seg : bp) {
            if (c <= seg[1]) {
                // IAQI = (IAQI_hi-IAQI_lo)/(BP_hi-BP_lo) * (C-BP_lo) + IAQI_lo
                double iaqi = (seg[3] - seg[2]) / (seg[1] - seg[0]) * (c - seg[0]) + seg[2];
                return (int) Math.round(iaqi);
            }
        }
        double[] last = bp[bp.length - 1];
        return (int) Math.round(last[3]);
    }

    /** 计算 AQI：有值项 IAQI 取最大值；无任何值返回 null */
    public static Integer calcAQI(DetectSubmitDTO dto) {
        int max = -1;
        if (dto.getPm25() != null) max = Math.max(max, iaqi(dto.getPm25().doubleValue(), PM25_BP));
        if (dto.getPm10() != null) max = Math.max(max, iaqi(dto.getPm10().doubleValue(), PM10_BP));
        if (dto.getSo2() != null) max = Math.max(max, iaqi(dto.getSo2().doubleValue(), SO2_BP));
        if (dto.getNo2() != null) max = Math.max(max, iaqi(dto.getNo2().doubleValue(), NO2_BP));
        if (dto.getCo() != null) max = Math.max(max, iaqi(dto.getCo().doubleValue(), CO_BP));
        if (dto.getO3() != null) max = Math.max(max, iaqi(dto.getO3().doubleValue(), O3_BP));
        return max < 0 ? null : max;
    }
}