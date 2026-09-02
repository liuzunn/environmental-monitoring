package org.nep.nepsystem;

import org.junit.jupiter.api.Test;
import org.nep.nepsystem.bean.Admins;
import org.nep.nepsystem.bean.EventStatusLog;
import org.nep.nepsystem.bean.InspectionRecord;
import org.nep.nepsystem.bean.InspectionTask;
import org.nep.nepsystem.bean.SupervisionEvent;
import org.nep.nepsystem.bean.Users;
import org.nep.nepsystem.dao.AdminsDao;
import org.nep.nepsystem.dao.EventStatusLogDao;
import org.nep.nepsystem.dao.InspectionRecordDao;
import org.nep.nepsystem.dao.InspectionTaskDao;
import org.nep.nepsystem.dao.SupervisionEventDao;
import org.nep.nepsystem.dao.UsersDao;
import org.nep.nepsystem.dto.AssignEventDTO;
import org.nep.nepsystem.dto.DetectSubmitDTO;
import org.nep.nepsystem.dto.InspectionRecordVO;
import org.nep.nepsystem.dto.SupervisionCreateDTO;
import org.nep.nepsystem.dto.SupervisionDetailVO;
import org.nep.nepsystem.exception.BizException;
import org.nep.nepsystem.service.InspectionTaskService;
import org.nep.nepsystem.service.SupervisionEventService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Phase 6 三端业务闭环联调测试：
 * NEPS(公众) -> NEPM(审核/派单/核实/关闭) -> NEPG(接单/检测/提交) 全链路 + 异常场景
 */
@SpringBootTest(properties = {"simulator.enabled=false", "quality.scan.enabled=false", "alert.auto-resolve-hold-ms=0"})
@Transactional
@AutoConfigureMockMvc
class ClosedLoopTests {

    @Autowired private AdminsDao adminsDao;
    @Autowired private UsersDao usersDao;
    @Autowired private SupervisionEventDao supervisionEventDao;
    @Autowired private InspectionTaskDao inspectionTaskDao;
    @Autowired private InspectionRecordDao inspectionRecordDao;
    @Autowired private EventStatusLogDao eventStatusLogDao;
    @Autowired private SupervisionEventService supervisionEventService;
    @Autowired private InspectionTaskService inspectionTaskService;
    @Autowired private MockMvc mockMvc;

    private String uid = String.valueOf(System.nanoTime());

    private Admins insertAdmin() {
        Admins a = new Admins();
        a.setAdminCode("e2e-cl-adm-" + uid + "-" + System.nanoTime());
        a.setPassword("123456");
        adminsDao.insert(a);
        return a;
    }

    private Users insertUser(String tag) {
        Users u = new Users();
        u.setUsername("e2e-cl-" + tag + "-" + uid + "-" + System.nanoTime());
        u.setPassword("123456");
        u.setNickname("闭环-" + tag);
        u.setRole("USER");
        u.setStatus(1);
        u.setCreateTime(new Date());
        usersDao.insert(u);
        return u;
    }

    /** 执行完整 15 步业务链，返回 {event, task} */
    private Object[] runFullLoop() {
        Admins admin = insertAdmin();
        Users citizen = insertUser("citizen");
        Users worker = insertUser("worker");

        // 1-2 公众提交
        SupervisionCreateDTO dto = new SupervisionCreateDTO();
        dto.setEventType("POLLUTION");
        dto.setTitle("闭环ENV-001");
        dto.setDescription("全链路联调事件");
        dto.setLocation("联调位置");
        dto.setLevel("WARN");
        SupervisionEvent e = supervisionEventService.create(citizen.getId(), dto);
        assertEquals("PENDING_REVIEW", e.getStatus());

        // 5 审核
        supervisionEventService.approve(e.getId(), admin.getAdminId(), "属实");
        assertEquals("APPROVED", supervisionEventDao.selectById(e.getId()).getStatus());

        // 6 派单
        AssignEventDTO adto = new AssignEventDTO();
        adto.setAssigneeId(worker.getId());
        adto.setPriority("HIGH");
        adto.setDeadline(new Date(System.currentTimeMillis() + 86400000L));
        supervisionEventService.assign(e.getId(), admin.getAdminId(), adto);
        InspectionTask task = inspectionTaskDao.selectOne(
                new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<InspectionTask>().eq("event_id", e.getId()));
        assertNotNull(task);
        assertEquals("ASSIGNED", task.getStatus());

        // 8 接单 / 9 开始
        inspectionTaskService.accept(task.getId(), worker.getId());
        inspectionTaskService.start(task.getId(), worker.getId());

        // 10-11 提交检测（六项 + AQI）
        DetectSubmitDTO det = new DetectSubmitDTO();
        det.setPm25(new BigDecimal("45.5"));
        det.setPm10(new BigDecimal("95.0"));
        det.setSo2(new BigDecimal("20.0"));
        det.setNo2(new BigDecimal("35.0"));
        det.setCo(new BigDecimal("0.8"));
        det.setO3(new BigDecimal("88.0"));
        det.setContent("现场检测记录-闭环");
        det.setImages(java.util.Arrays.asList("loop1.jpg"));
        inspectionTaskService.submit(task.getId(), worker.getId(), det);
        assertEquals("INSPECTED", inspectionTaskDao.selectById(task.getId()).getStatus());
        assertEquals("INSPECTED", supervisionEventDao.selectById(e.getId()).getStatus());

        // 12 管理员查看检测结果
        List<InspectionRecordVO> recs = inspectionTaskService.records(admin.getAdminId(), task.getId());
        assertEquals(1, recs.size());
        assertNotNull(recs.get(0).getAqiValue(), "AQI 已计算");

        // 13 核实
        inspectionTaskService.verify(admin.getAdminId(), task.getId(), "数据合理");
        assertEquals("VERIFIED", inspectionTaskDao.selectById(task.getId()).getStatus());
        assertEquals("VERIFIED", supervisionEventDao.selectById(e.getId()).getStatus());

        // 14 关闭
        inspectionTaskService.close(admin.getAdminId(), task.getId(), "事件处置完毕");
        assertEquals("CLOSED", inspectionTaskDao.selectById(task.getId()).getStatus());
        assertEquals("CLOSED", supervisionEventDao.selectById(e.getId()).getStatus());

        // 15 公众详情可见时间线完整（8 条）
        SupervisionDetailVO detail = supervisionEventService.detail(e.getId(), citizen.getId(), null);
        assertEquals(8, detail.getStatusLogs().size(), "创建/审核/派单/接单/开始/提交/核实/关闭 = 8 条时间线");
        assertEquals("CLOSED", detail.getEvent().getStatus());
        assertNotNull(detail.getTaskId());
        assertNotNull(detail.getTaskNo());
        return new Object[]{e, task};
    }

    // ---------- 1. 正常全流程 ----------

    @Test
    void fullClosedLoop() {
        Object[] r = runFullLoop();
        SupervisionEvent e = (SupervisionEvent) r[0];
        InspectionTask task = (InspectionTask) r[1];
        // 数据库关系：事件->任务->记录 完整
        InspectionRecord rec = inspectionRecordDao.selectOne(
                new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<InspectionRecord>().eq("task_id", task.getId()));
        assertNotNull(rec);
        assertEquals(e.getId(), task.getEventId());
        // 时间线状态序列
        List<EventStatusLog> logs = eventStatusLogDao.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<EventStatusLog>().eq("event_id", e.getId()).orderByAsc("id"));
        String[] expected = {"PENDING_REVIEW", "APPROVED", "ASSIGNED", "ACCEPTED", "INSPECTING", "INSPECTED", "VERIFIED", "CLOSED"};
        for (int i = 0; i < expected.length; i++) {
            assertEquals(expected[i], logs.get(i).getToStatus(), "第" + (i + 1) + "条日志应为 " + expected[i]);
        }
    }

    // ---------- 2. 重复提交 ----------

    @Test
    void duplicateRejections() {
        Admins admin = insertAdmin();
        Users citizen = insertUser("dup-c");
        Users worker = insertUser("dup-w");
        SupervisionEvent e = supervisionEventService.create(citizen.getId(), newDto("重复事件"));
        supervisionEventService.approve(e.getId(), admin.getAdminId(), null);
        // 重复审核（已 APPROVED 再 approve）—— 需新建事件到 PENDING_REVIEW 才能测重复审核
        SupervisionEvent e2 = supervisionEventService.create(citizen.getId(), newDto("重复审核"));
        supervisionEventService.approve(e2.getId(), admin.getAdminId(), null);
        assertThrows(BizException.class, () -> supervisionEventService.approve(e2.getId(), admin.getAdminId(), null), "重复审核拒绝");
        // 重复派单
        AssignEventDTO adto = new AssignEventDTO();
        adto.setAssigneeId(worker.getId());
        supervisionEventService.assign(e.getId(), admin.getAdminId(), adto);
        assertThrows(BizException.class, () -> supervisionEventService.assign(e.getId(), admin.getAdminId(), adto), "重复派单拒绝");
        // 任务重复接收
        InspectionTask task = inspectionTaskDao.selectOne(
                new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<InspectionTask>().eq("event_id", e.getId()));
        inspectionTaskService.accept(task.getId(), worker.getId());
        assertThrows(BizException.class, () -> inspectionTaskService.accept(task.getId(), worker.getId()), "重复接收拒绝");
        // 检测重复提交
        inspectionTaskService.start(task.getId(), worker.getId());
        DetectSubmitDTO det = new DetectSubmitDTO();
        det.setPm25(new BigDecimal("50"));
        inspectionTaskService.submit(task.getId(), worker.getId(), det);
        assertThrows(BizException.class, () -> inspectionTaskService.submit(task.getId(), worker.getId(), det), "检测重复提交拒绝");
        // 核实重复
        inspectionTaskService.verify(admin.getAdminId(), task.getId(), null);
        assertThrows(BizException.class, () -> inspectionTaskService.verify(admin.getAdminId(), task.getId(), null), "重复核实拒绝");
        // 关闭重复
        inspectionTaskService.close(admin.getAdminId(), task.getId(), null);
        assertThrows(BizException.class, () -> inspectionTaskService.close(admin.getAdminId(), task.getId(), null), "重复关闭拒绝");
    }

    private SupervisionCreateDTO newDto(String title) {
        SupervisionCreateDTO dto = new SupervisionCreateDTO();
        dto.setEventType("POLLUTION");
        dto.setTitle(title);
        dto.setDescription("描述");
        dto.setLocation("位置");
        dto.setLevel("WARN");
        return dto;
    }

    // ---------- 3. 越权访问 ----------

    @Test
    void unauthorizedAccess() {
        Admins admin = insertAdmin();
        Users citizen = insertUser("unauth-c");
        Users other = insertUser("unauth-o");
        Users worker = insertUser("unauth-w");
        Users otherWorker = insertUser("unauth-ow");
        SupervisionEvent e = supervisionEventService.create(citizen.getId(), newDto("越权事件"));
        // 公众看他人事件
        assertThrows(BizException.class, () -> supervisionEventService.detail(e.getId(), other.getId(), null));
        // 网格员操作他人任务
        supervisionEventService.approve(e.getId(), admin.getAdminId(), null);
        AssignEventDTO adto = new AssignEventDTO();
        adto.setAssigneeId(worker.getId());
        supervisionEventService.assign(e.getId(), admin.getAdminId(), adto);
        InspectionTask task = inspectionTaskDao.selectOne(
                new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<InspectionTask>().eq("event_id", e.getId()));
        assertThrows(BizException.class, () -> inspectionTaskService.accept(task.getId(), otherWorker.getId()), "他人任务接单拒绝");
        assertThrows(BizException.class, () -> inspectionTaskService.detailMine(task.getId(), otherWorker.getId()), "他人任务详情拒绝");
        // 非管理员核实/关闭/查看记录
        assertThrows(BizException.class, () -> inspectionTaskService.verify(null, task.getId(), null));
        assertThrows(BizException.class, () -> inspectionTaskService.verify(worker.getId(), task.getId(), null), "网格员冒充管理员核实拒绝");
        assertThrows(BizException.class, () -> inspectionTaskService.close(other.getId(), task.getId(), null));
        assertThrows(BizException.class, () -> inspectionTaskService.records(null, task.getId()));
        // 公众查管理接口（无X-Admin-Id）
        assertThrows(BizException.class, () -> inspectionTaskService.records(citizen.getId(), task.getId()), "公众查看检测记录拒绝");
    }

    // ---------- 4. 错误状态转换 ----------

    @Test
    void wrongStateTransitions() {
        Admins admin = insertAdmin();
        Users citizen = insertUser("state-c");
        Users worker = insertUser("state-w");
        SupervisionEvent e = supervisionEventService.create(citizen.getId(), newDto("状态机事件"));
        // PENDING_REVIEW 直接派单
        AssignEventDTO adto = new AssignEventDTO();
        adto.setAssigneeId(worker.getId());
        assertThrows(BizException.class, () -> supervisionEventService.assign(e.getId(), admin.getAdminId(), adto));
        // PENDING_REVIEW 直接驳回后再审核
        supervisionEventService.reject(e.getId(), admin.getAdminId(), "驳回");
        assertThrows(BizException.class, () -> supervisionEventService.approve(e.getId(), admin.getAdminId(), null), "已驳回不可审核");
        // 新事件：审核后跳过接单直接开始
        SupervisionEvent e2 = supervisionEventService.create(citizen.getId(), newDto("跳步事件"));
        supervisionEventService.approve(e2.getId(), admin.getAdminId(), null);
        AssignEventDTO adto2 = new AssignEventDTO();
        adto2.setAssigneeId(worker.getId());
        supervisionEventService.assign(e2.getId(), admin.getAdminId(), adto2);
        InspectionTask task = inspectionTaskDao.selectOne(
                new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<InspectionTask>().eq("event_id", e2.getId()));
        assertThrows(BizException.class, () -> inspectionTaskService.start(task.getId(), worker.getId()), "未接单不可开始");
        // 未开始直接提交
        inspectionTaskService.accept(task.getId(), worker.getId());
        DetectSubmitDTO det = new DetectSubmitDTO();
        det.setPm25(new BigDecimal("50"));
        assertThrows(BizException.class, () -> inspectionTaskService.submit(task.getId(), worker.getId(), det), "未开始不可提交");
        // 未提交直接核实
        assertThrows(BizException.class, () -> inspectionTaskService.verify(admin.getAdminId(), task.getId(), null), "未提交不可核实");
        // 未核实直接关闭
        inspectionTaskService.start(task.getId(), worker.getId());
        inspectionTaskService.submit(task.getId(), worker.getId(), det);
        assertThrows(BizException.class, () -> inspectionTaskService.close(admin.getAdminId(), task.getId(), null), "未核实不可关闭");
    }

    // ---------- 5. 不存在 ID ----------

    @Test
    void missingIds() {
        Admins admin = insertAdmin();
        Users citizen = insertUser("miss");
        Users worker = insertUser("miss-w");
        assertThrows(BizException.class, () -> supervisionEventService.approve(99999999L, admin.getAdminId(), null));
        assertThrows(BizException.class, () -> supervisionEventService.assign(99999999L, admin.getAdminId(), new AssignEventDTO()));
        assertThrows(BizException.class, () -> supervisionEventService.detail(99999999L, citizen.getId(), null));
        assertThrows(BizException.class, () -> inspectionTaskService.accept(99999999L, worker.getId()));
        assertThrows(BizException.class, () -> inspectionTaskService.submit(99999999L, worker.getId(), new DetectSubmitDTO()));
        assertThrows(BizException.class, () -> inspectionTaskService.verify(admin.getAdminId(), 99999999L, null));
        assertThrows(BizException.class, () -> inspectionTaskService.close(admin.getAdminId(), 99999999L, null));
        assertThrows(BizException.class, () -> inspectionTaskService.records(admin.getAdminId(), 99999999L));
    }

    // ---------- 6. HTTP 全链路 ----------

    @Test
    void httpClosedLoop() throws Exception {
        Admins admin = insertAdmin();
        Users citizen = insertUser("http-c");
        Users worker = insertUser("http-w");

        // 1-2 公众提交
        String evBody = "{\"eventType\":\"NOISE\",\"title\":\"HTTP闭环ENV-001\",\"description\":\"desc\",\"location\":\"loc\",\"level\":\"WARN\"}";
        String resp = mockMvc.perform(post("/api/supervision")
                        .header("X-User-Id", String.valueOf(citizen.getId()))
                        .contentType(MediaType.APPLICATION_JSON).content(evBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("PENDING_REVIEW"))
                .andReturn().getResponse().getContentAsString();
        String eid = new com.fasterxml.jackson.databind.ObjectMapper().readTree(resp).path("data").path("id").asText();
        String adminH = String.valueOf(admin.getAdminId());
        String workerH = String.valueOf(worker.getId());
        String citizenH = String.valueOf(citizen.getId());

        // 5 审核 / 6 派单
        mockMvc.perform(put("/api/supervision/" + eid + "/approve").header("X-Admin-Id", adminH).contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(jsonPath("$.data.status").value("APPROVED"));
        mockMvc.perform(put("/api/supervision/" + eid + "/assign").header("X-Admin-Id", adminH)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"assigneeId\":" + worker.getId() + ",\"priority\":\"HIGH\"}"))
                .andExpect(jsonPath("$.data.status").value("ASSIGNED"));

        // 7-9 网格员查看/接单/开始
        String mineResp = mockMvc.perform(get("/api/tasks/mine").header("X-User-Id", workerH))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(1))
                .andReturn().getResponse().getContentAsString();
        String tid = new com.fasterxml.jackson.databind.ObjectMapper().readTree(mineResp).path("data").path("records").get(0).path("id").asText();
        mockMvc.perform(put("/api/tasks/mine/" + tid + "/accept").header("X-User-Id", workerH))
                .andExpect(jsonPath("$.data.status").value("ACCEPTED"));
        mockMvc.perform(put("/api/tasks/mine/" + tid + "/start").header("X-User-Id", workerH))
                .andExpect(jsonPath("$.data.status").value("INSPECTING"));

        // 10-11 提交检测（六项）
        String detBody = "{\"pm25\":66.0,\"pm10\":120.0,\"so2\":15.0,\"no2\":48.0,\"co\":1.0,\"o3\":100.0,\"content\":\"闭环检测\"}";
        mockMvc.perform(put("/api/tasks/mine/" + tid + "/submit").header("X-User-Id", workerH)
                        .contentType(MediaType.APPLICATION_JSON).content(detBody))
                .andExpect(jsonPath("$.data.status").value("INSPECTED"));

        // 12 管理员查看检测结果
        mockMvc.perform(get("/api/tasks/" + tid + "/records").header("X-Admin-Id", adminH))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].aqiValue").isNumber());

        // 13 核实 / 14 关闭
        mockMvc.perform(put("/api/tasks/" + tid + "/verify").header("X-Admin-Id", adminH))
                .andExpect(jsonPath("$.data.status").value("VERIFIED"));
        mockMvc.perform(put("/api/tasks/" + tid + "/close").header("X-Admin-Id", adminH).param("remark", "处置完毕"))
                .andExpect(jsonPath("$.data.status").value("CLOSED"));

        // 15 公众查看：状态已完成 + 时间线 8 条
        mockMvc.perform(get("/api/supervision/" + eid).header("X-User-Id", citizenH))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.event.status").value("CLOSED"))
                .andExpect(jsonPath("$.data.statusLogs.length()").value(8))
                .andExpect(jsonPath("$.data.taskNo").isNotEmpty());
    }
}
