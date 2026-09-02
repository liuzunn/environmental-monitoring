package org.nep.nepsystem;

import org.junit.jupiter.api.Test;
import org.nep.nepsystem.bean.Admins;
import org.nep.nepsystem.bean.EventStatusLog;
import org.nep.nepsystem.bean.Grids;
import org.nep.nepsystem.bean.InspectionRecord;
import org.nep.nepsystem.bean.InspectionTask;
import org.nep.nepsystem.bean.SupervisionEvent;
import org.nep.nepsystem.bean.Users;
import org.nep.nepsystem.common.PageResult;
import org.nep.nepsystem.dao.AdminsDao;
import org.nep.nepsystem.dao.EventStatusLogDao;
import org.nep.nepsystem.dao.GridsDao;
import org.nep.nepsystem.dao.InspectionRecordDao;
import org.nep.nepsystem.dao.InspectionTaskDao;
import org.nep.nepsystem.dao.SupervisionEventDao;
import org.nep.nepsystem.dao.UsersDao;
import org.nep.nepsystem.dto.DetectSubmitDTO;
import org.nep.nepsystem.dto.SupervisionCreateDTO;
import org.nep.nepsystem.dto.AssignEventDTO;
import org.nep.nepsystem.dto.InspectionTaskCreateDTO;
import org.nep.nepsystem.dto.TaskDetailVO;
import org.nep.nepsystem.dto.TaskStatsVO;
import org.nep.nepsystem.dto.TaskVO;
import org.nep.nepsystem.exception.BizException;
import org.nep.nepsystem.service.GridsService;
import org.nep.nepsystem.service.InspectionTaskService;
import org.nep.nepsystem.service.impl.InspectionTaskServiceImpl;
import org.nep.nepsystem.service.SupervisionEventService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * NEPG 网格员端测试（Phase 5 新增）：
 * 登录/接任务/开始检测/提交检测(含AQI计算)/历史任务/越权/状态机顺序
 */
@SpringBootTest(properties = {"simulator.enabled=false", "quality.scan.enabled=false", "alert.auto-resolve-hold-ms=0"})
@Transactional
@AutoConfigureMockMvc
class GridWorkerTests {

    @Autowired private AdminsDao adminsDao;
    @Autowired private EventStatusLogDao eventStatusLogDao;
    @Autowired private UsersDao usersDao;
    @Autowired private GridsDao gridsDao;
    @Autowired private InspectionTaskDao inspectionTaskDao;
    @Autowired private InspectionRecordDao inspectionRecordDao;
    @Autowired private SupervisionEventDao supervisionEventDao;
    @Autowired private GridsService gridsService;
    @Autowired private InspectionTaskService inspectionTaskService;
    @Autowired private SupervisionEventService supervisionEventService;
    @Autowired private MockMvc mockMvc;

    private String uid = String.valueOf(System.nanoTime());

    private Admins insertAdmin() {
        Admins a = new Admins();
        a.setAdminCode("e2e-gw-adm-" + uid + "-" + System.nanoTime());
        a.setPassword("123456");
        adminsDao.insert(a);
        return a;
    }

    private Users insertUser(String tag) {
        Users u = new Users();
        u.setUsername("e2e-gw-" + tag + "-" + uid);
        u.setPassword("123456");
        u.setNickname("网格员-" + tag);
        u.setRole("USER");
        u.setStatus(1);
        u.setCreateTime(new Date());
        usersDao.insert(u);
        return u;
    }

    private InspectionTask createAssignedTask(Users assignee, String desc) {
        Grids g = new Grids();
        g.setGridCode("GRID-GW-" + uid + "-" + System.nanoTime());
        g.setGridName("网格员测试网格");
        g.setStatus(1);
        g.setCreateTime(new Date());
        gridsDao.insert(g);
        InspectionTaskCreateDTO dto = new InspectionTaskCreateDTO();
        dto.setGridId(g.getId());
        dto.setAssigneeId(assignee.getId());
        dto.setPriority("HIGH");
        dto.setDeadline(new Date(System.currentTimeMillis() + 86400000L));
        dto.setDescription(desc);
        return inspectionTaskService.create(insertAdmin().getAdminId(), dto);
    }

    private SupervisionEvent createEventWithAdminFlow(Users submitter, Admins admin, Users assignee) {
        org.nep.nepsystem.dto.SupervisionCreateDTO dto = new org.nep.nepsystem.dto.SupervisionCreateDTO();
        dto.setEventType("POLLUTION");
        dto.setTitle("网格员端-扬尘事件");
        dto.setDescription("工地扬尘");
        dto.setLocation("东区工地");
        dto.setLevel("WARN");
        SupervisionEvent e = supervisionEventService.create(submitter.getId(), dto);
        supervisionEventService.approve(e.getId(), admin.getAdminId(), null);
        org.nep.nepsystem.dto.AssignEventDTO adto = new org.nep.nepsystem.dto.AssignEventDTO();
        adto.setAssigneeId(assignee.getId());
        adto.setPriority("HIGH");
        supervisionEventService.assign(e.getId(), admin.getAdminId(), adto);
        return e;
    }

    // ---------- 1. AQI 计算 ----------

    @Test
    void aqiCalculation() {
        DetectSubmitDTO dto = new DetectSubmitDTO();
        dto.setPm25(new BigDecimal("35"));
        assertEquals(50, InspectionTaskServiceImpl.calcAQI(dto), "PM2.5=35 -> IAQI 50");
        dto.setPm25(new BigDecimal("75"));
        assertEquals(100, InspectionTaskServiceImpl.calcAQI(dto), "PM2.5=75 -> IAQI 100");
        // 多项取最大
        DetectSubmitDTO dto2 = new DetectSubmitDTO();
        dto2.setPm25(new BigDecimal("35"));   // 50
        dto2.setPm10(new BigDecimal("300"));  // ~175
        dto2.setNo2(new BigDecimal("500"));   // ~270+
        assertEquals(277, InspectionTaskServiceImpl.calcAQI(dto2), "NO2=500 -> IAQI 277 为最大");
        // 全空 -> null
        assertNull(InspectionTaskServiceImpl.calcAQI(new DetectSubmitDTO()));
    }

    // ---------- 2. 接任务 / 开始 / 提交 状态机 ----------

    @Test
    void fullWorkflowAcceptStartSubmit() {
        Users worker = insertUser("flow");
        InspectionTask t = createAssignedTask(worker, "例行巡检任务");
        assertEquals("ASSIGNED", t.getStatus());

        // 未接收就 start -> 拒绝
        assertThrows(BizException.class, () -> inspectionTaskService.start(t.getId(), worker.getId()));
        // 接收
        InspectionTask accepted = inspectionTaskService.accept(t.getId(), worker.getId());
        assertEquals("ACCEPTED", accepted.getStatus());
        // 重复接收拒绝
        assertThrows(BizException.class, () -> inspectionTaskService.accept(t.getId(), worker.getId()));
        // 未开始就提交 -> 拒绝
        DetectSubmitDTO dto = new DetectSubmitDTO();
        dto.setPm25(new BigDecimal("45"));
        assertThrows(BizException.class, () -> inspectionTaskService.submit(t.getId(), worker.getId(), dto));
        // 开始
        InspectionTask inspecting = inspectionTaskService.start(t.getId(), worker.getId());
        assertEquals("INSPECTING", inspecting.getStatus());
        // 提交
        dto.setPm25(new BigDecimal("45"));
        dto.setPm10(new BigDecimal("80"));
        dto.setContent("现场无明显异味，颗粒物偏高");
        dto.setLat(new BigDecimal("31.2304"));
        dto.setLng(new BigDecimal("121.4737"));
        dto.setImages(Collections.singletonList("site1.jpg"));
        InspectionTask done = inspectionTaskService.submit(t.getId(), worker.getId(), dto);
        assertEquals("INSPECTED", done.getStatus());
        // inspection_record 落库
        List<InspectionRecord> recs = inspectionRecordDao.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<InspectionRecord>().eq("task_id", t.getId()));
        assertEquals(1, recs.size());
        assertEquals(0, new BigDecimal("45").compareTo(recs.get(0).getPm25()));
        assertEquals(0, new BigDecimal("80").compareTo(recs.get(0).getPm10()));
        assertEquals(65, recs.get(0).getAqiValue(), "PM2.5=45->IAQI63, PM10=80->IAQI65, 取最大65");
        assertEquals("site1.jpg", recs.get(0).getImages());
        // 已完成不可再操作
        assertThrows(BizException.class, () -> inspectionTaskService.submit(t.getId(), worker.getId(), dto));
    }

    // ---------- 3. 事件状态同步 ----------

    @Test
    void eventStateSyncsWithTask() {
        Admins admin = insertAdmin();
        Users submitter = insertUser("sub");
        Users worker = insertUser("sync");
        SupervisionEvent e = createEventWithAdminFlow(submitter, admin, worker);
        // 事件经审核+派单后为 ASSIGNED（以库中最新状态为准）
        assertEquals("ASSIGNED", supervisionEventDao.selectById(e.getId()).getStatus());
        InspectionTask t = inspectionTaskDao.selectOne(
                new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<InspectionTask>().eq("event_id", e.getId()));
        assertNotNull(t);
        inspectionTaskService.accept(t.getId(), worker.getId());
        assertEquals("ACCEPTED", supervisionEventDao.selectById(e.getId()).getStatus());
        inspectionTaskService.start(t.getId(), worker.getId());
        assertEquals("INSPECTING", supervisionEventDao.selectById(e.getId()).getStatus());
        DetectSubmitDTO dto = new DetectSubmitDTO();
        dto.setPm25(new BigDecimal("60"));
        inspectionTaskService.submit(t.getId(), worker.getId(), dto);
        assertEquals("INSPECTED", supervisionEventDao.selectById(e.getId()).getStatus());
    }

    // ---------- 4. 我的任务/统计/详情 ----------

    @Test
    void mineListStatsAndDetail() {
        Users worker = insertUser("mine");
        Users other = insertUser("other");
        InspectionTask t1 = createAssignedTask(worker, "任务甲");
        InspectionTask t2 = createAssignedTask(worker, "任务乙");
        InspectionTask tOther = createAssignedTask(other, "他人任务");

        // 列表仅本人
        PageResult<TaskVO> mine = inspectionTaskService.pageMine(worker.getId(), 1, 10, null, null);
        assertEquals(2, mine.getTotal());
        assertTrue(mine.getRecords().stream().allMatch(v -> v.getAssigneeId().equals(worker.getId())));
        // 统计
        TaskStatsVO stats = inspectionTaskService.mineStats(worker.getId());
        assertEquals(2, stats.getPendingAccept(), "两个待接收");
        assertEquals(0, stats.getProcessing());
        // 详情本人可看
        TaskDetailVO detail = inspectionTaskService.detailMine(t1.getId(), worker.getId());
        assertNotNull(detail.getTask());
        // 他人任务越权
        assertThrows(BizException.class, () -> inspectionTaskService.detailMine(tOther.getId(), worker.getId()));
        assertThrows(BizException.class, () -> inspectionTaskService.accept(tOther.getId(), worker.getId()));
        // 无身份
        assertThrows(BizException.class, () -> inspectionTaskService.pageMine(null, 1, 10, null, null));
        // 状态过滤：接收后按 ACCEPTED 过滤
        inspectionTaskService.accept(t1.getId(), worker.getId());
        PageResult<TaskVO> acceptedOnly = inspectionTaskService.pageMine(worker.getId(), 1, 10, "ACCEPTED", null);
        assertEquals(1, acceptedOnly.getTotal());
    }

    // ---------- 5. HTTP 接口层 + 越权 ----------

    @Test
    void gridEndpointsWork() throws Exception {
        Users worker = insertUser("http");
        Users other = insertUser("http-other");
        InspectionTask t = createAssignedTask(worker, "HTTP任务");

        // 未登录 -> 401
        mockMvc.perform(get("/api/tasks/mine"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(401));

        // 列表 + 统计
        mockMvc.perform(get("/api/tasks/mine").header("X-User-Id", String.valueOf(worker.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.total").value(1));
        mockMvc.perform(get("/api/tasks/mine/stats").header("X-User-Id", String.valueOf(worker.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.pendingAccept").value(1));

        // 接任务
        mockMvc.perform(put("/api/tasks/mine/" + t.getId() + "/accept").header("X-User-Id", String.valueOf(worker.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("ACCEPTED"));
        // 开始
        mockMvc.perform(put("/api/tasks/mine/" + t.getId() + "/start").header("X-User-Id", String.valueOf(worker.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("INSPECTING"));
        // 提交（JSON body）
        String body = "{\"pm25\":55.0,\"pm10\":120.0,\"content\":\"检测完成\",\"images\":[\"a.jpg\"]}";
        mockMvc.perform(put("/api/tasks/mine/" + t.getId() + "/submit")
                        .header("X-User-Id", String.valueOf(worker.getId()))
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("INSPECTED"));
        // 历史任务
        mockMvc.perform(get("/api/tasks/mine?status=INSPECTED").header("X-User-Id", String.valueOf(worker.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(1));

        // 越权：他人看详情/操作 -> 403
        mockMvc.perform(get("/api/tasks/mine/" + t.getId()).header("X-User-Id", String.valueOf(other.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(403));
        // 网格员冒充管理员调用管理员接口 -> 403
        mockMvc.perform(get("/api/grids/list").header("X-Admin-Id", String.valueOf(worker.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(403));
        // 网格员创建任务 -> 403（管理员接口）
        mockMvc.perform(post("/api/tasks").header("X-Admin-Id", String.valueOf(worker.getId()))
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(403));
    }
    // ---------- BUG-001 回归：事务回滚一致性 ----------

    @Test
    void submitRollsBackOnDatabaseError() {
        Admins admin = insertAdmin();
        Users citizen = insertUser("rb-c");
        Users worker = insertUser("rb-w");
        // 带事件的完整任务链（事件同步分支也要回滚）
        SupervisionCreateDTO dto = new SupervisionCreateDTO();
        dto.setEventType("POLLUTION");
        dto.setTitle("回滚事件");
        dto.setDescription("回滚");
        dto.setLocation("回滚位置");
        dto.setLevel("WARN");
        SupervisionEvent e = supervisionEventService.create(citizen.getId(), dto);
        supervisionEventService.approve(e.getId(), admin.getAdminId(), null);
        AssignEventDTO adto = new AssignEventDTO();
        adto.setAssigneeId(worker.getId());
        supervisionEventService.assign(e.getId(), admin.getAdminId(), adto);
        InspectionTask task = inspectionTaskDao.selectOne(
                new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<InspectionTask>().eq("event_id", e.getId()));
        inspectionTaskService.accept(task.getId(), worker.getId());
        inspectionTaskService.start(task.getId(), worker.getId());
        long logBefore = eventStatusLogDao.selectCount(
                new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<EventStatusLog>().eq("event_id", e.getId()));

        // images 超长(>1000) 触发 Data too long → submit 抛异常
        DetectSubmitDTO bad = new DetectSubmitDTO();
        bad.setPm25(new BigDecimal("50"));
        bad.setImages(java.util.Collections.singletonList(new String(new char[1200]).replace('\0', 'x')));
        assertThrows(RuntimeException.class, () -> inspectionTaskService.submit(task.getId(), worker.getId(), bad),
                "超长 images 应触发数据库异常");

        // 事务回滚验证：无记录残留；任务/事件仍 INSPECTING；日志未新增
        assertEquals(0, inspectionRecordDao.selectCount(
                new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<InspectionRecord>().eq("task_id", task.getId())),
                "回滚后不应有检测记录");
        assertEquals("INSPECTING", inspectionTaskDao.selectById(task.getId()).getStatus(), "任务状态应回滚到 INSPECTING");
        assertEquals("INSPECTING", supervisionEventDao.selectById(e.getId()).getStatus(), "事件状态应回滚到 INSPECTING");
        assertEquals(logBefore, eventStatusLogDao.selectCount(
                new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<EventStatusLog>().eq("event_id", e.getId())).longValue(),
                "回滚后状态日志不应新增");
    }
}