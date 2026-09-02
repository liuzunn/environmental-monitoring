package org.nep.nepsystem;

import org.junit.jupiter.api.Test;
import org.nep.nepsystem.bean.Admins;
import org.nep.nepsystem.bean.GridMember;
import org.nep.nepsystem.bean.Grids;
import org.nep.nepsystem.bean.InspectionTask;
import org.nep.nepsystem.bean.SupervisionEvent;
import org.nep.nepsystem.bean.Users;
import org.nep.nepsystem.common.PageResult;
import org.nep.nepsystem.dao.AdminsDao;
import org.nep.nepsystem.dao.EventStatusLogDao;
import org.nep.nepsystem.dao.GridMemberDao;
import org.nep.nepsystem.dao.GridsDao;
import org.nep.nepsystem.dao.InspectionTaskDao;
import org.nep.nepsystem.dao.SupervisionEventDao;
import org.nep.nepsystem.dao.UsersDao;
import org.nep.nepsystem.dto.AssignEventDTO;
import org.nep.nepsystem.dto.AssigneeStatsVO;
import org.nep.nepsystem.dto.GridMemberAssignDTO;
import org.nep.nepsystem.dto.InspectionTaskCreateDTO;
import org.nep.nepsystem.dto.SupervisionCreateDTO;
import org.nep.nepsystem.dto.TaskVO;
import org.nep.nepsystem.dto.WorkbenchStatsDTO;
import org.nep.nepsystem.exception.BizException;
import org.nep.nepsystem.service.GridMemberService;
import org.nep.nepsystem.service.GridsService;
import org.nep.nepsystem.service.InspectionTaskService;
import org.nep.nepsystem.service.SupervisionEventService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * NEPM 管理端业务测试（Phase 4 新增）：
 * 管理员登录/审核/派单/任务/网格/网格员 + 越权拒绝
 */
@SpringBootTest(properties = {"simulator.enabled=false", "quality.scan.enabled=false", "alert.auto-resolve-hold-ms=0"})
@Transactional
@AutoConfigureMockMvc
class GridTaskBusinessTests {

    @Autowired private AdminsDao adminsDao;
    @Autowired private UsersDao usersDao;
    @Autowired private GridsDao gridsDao;
    @Autowired private GridMemberDao gridMemberDao;
    @Autowired private InspectionTaskDao inspectionTaskDao;
    @Autowired private SupervisionEventDao supervisionEventDao;
    @Autowired private EventStatusLogDao eventStatusLogDao;
    @Autowired private SupervisionEventService supervisionEventService;
    @Autowired private GridsService gridsService;
    @Autowired private GridMemberService gridMemberService;
    @Autowired private InspectionTaskService inspectionTaskService;
    @Autowired private MockMvc mockMvc;

    private String uid = String.valueOf(System.nanoTime());

    private Admins insertAdmin(String tag) {
        Admins a = new Admins();
        a.setAdminCode("e2e-adm-" + tag + "-" + uid);
        a.setPassword("123456");
        adminsDao.insert(a);
        return a;
    }

    private Users insertUser(String tag, int status) {
        Users u = new Users();
        u.setUsername("e2e-gt-" + tag + "-" + uid);
        u.setPassword("123456");
        u.setNickname("业务测试-" + tag);
        u.setRole("USER");
        u.setStatus(status);
        u.setCreateTime(new Date());
        usersDao.insert(u);
        return u;
    }

    private Grids insertGrid(String tag, int status) {
        Grids g = new Grids();
        g.setGridCode("GRID-" + tag + "-" + uid);
        g.setGridName("测试网格-" + tag);
        g.setStatus(status);
        g.setCreateTime(new Date());
        gridsDao.insert(g);
        return g;
    }

    private SupervisionEvent createEvent(Users u, String title) {
        SupervisionCreateDTO dto = new SupervisionCreateDTO();
        dto.setEventType("POLLUTION");
        dto.setTitle(title);
        dto.setDescription("测试事件");
        dto.setLocation("测试位置");
        dto.setLevel("WARN");
        return supervisionEventService.create(u.getId(), dto);
    }

    // ---------- 1. 网格管理 ----------

    @Test
    void gridCrudAndStatus() {
        Admins admin = insertAdmin("grid");
        // 新增
        Grids g = new Grids();
        g.setGridCode("GRID-NEW-" + uid);
        g.setGridName("新网格");
        Grids created = gridsService.add(admin.getAdminId(), g);
        assertNotNull(created.getId());
        assertEquals(1, created.getStatus());
        // 重复编号拒绝
        Grids dup = new Grids();
        dup.setGridCode("GRID-NEW-" + uid);
        dup.setGridName("重复");
        assertThrows(BizException.class, () -> gridsService.add(admin.getAdminId(), dup));
        // 列表
        List<Grids> all = gridsService.list(admin.getAdminId(), null, null);
        assertTrue(all.stream().anyMatch(x -> "新网格".equals(x.getGridName())));
        // 编辑
        created.setGridName("新网格-改");
        gridsService.update(admin.getAdminId(), created);
        assertEquals("新网格-改", gridsDao.selectById(created.getId()).getGridName());
        // 停用/启用
        gridsService.changeStatus(admin.getAdminId(), created.getId(), 0);
        assertEquals(0, gridsDao.selectById(created.getId()).getStatus());
        // 停用后分配成员应拒绝
        Users u = insertUser("griduser", 1);
        GridMemberAssignDTO dto = new GridMemberAssignDTO();
        dto.setGridId(created.getId());
        dto.setUserId(u.getId());
        assertThrows(BizException.class, () -> gridMemberService.assign(admin.getAdminId(), dto));
        // 启用后再分配成功
        gridsService.changeStatus(admin.getAdminId(), created.getId(), 1);
        gridMemberService.assign(admin.getAdminId(), dto);
        // 删除：有成员时拒绝
        assertThrows(BizException.class, () -> gridsService.delete(admin.getAdminId(), created.getId()));
        // 移除成员后可删
        GridMember m = gridMemberDao.selectOne(new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<GridMember>()
                .eq("grid_id", created.getId()).eq("user_id", u.getId()));
        gridMemberService.remove(admin.getAdminId(), m.getId());
        gridsService.delete(admin.getAdminId(), created.getId());
        assertNull(gridsDao.selectById(created.getId()));
        // 权限
        assertThrows(BizException.class, () -> gridsService.list(null, null, null));
    }

    // ---------- 2. 网格员管理 ----------

    @Test
    void gridMemberAssignAndList() {
        Admins admin = insertAdmin("member");
        Grids g1 = insertGrid("m1", 1);
        Grids g2 = insertGrid("m2", 1);
        Users u1 = insertUser("m1", 1);
        Users u2 = insertUser("m2", 1);
        Users disabled = insertUser("m3", 0);
        // 分配
        GridMemberAssignDTO dto1 = new GridMemberAssignDTO();
        dto1.setGridId(g1.getId());
        dto1.setUserId(u1.getId());
        gridMemberService.assign(admin.getAdminId(), dto1);
        GridMemberAssignDTO dto2 = new GridMemberAssignDTO();
        dto2.setGridId(g1.getId());
        dto2.setUserId(u2.getId());
        dto2.setRole("GRID_LEADER");
        gridMemberService.assign(admin.getAdminId(), dto2);
        // 重复分配拒绝
        assertThrows(BizException.class, () -> gridMemberService.assign(admin.getAdminId(), dto1));
        // 禁用用户拒绝
        GridMemberAssignDTO bad = new GridMemberAssignDTO();
        bad.setGridId(g2.getId());
        bad.setUserId(disabled.getId());
        assertThrows(BizException.class, () -> gridMemberService.assign(admin.getAdminId(), bad));
        // 列表（按网格过滤 + 名称）
        List<org.nep.nepsystem.dto.GridMemberVO> inG1 = gridMemberService.list(admin.getAdminId(), g1.getId(), null);
        assertEquals(2, inG1.size());
        assertTrue(inG1.stream().allMatch(v -> v.getGridName().equals(g1.getGridName())));
        // 关键字
        List<org.nep.nepsystem.dto.GridMemberVO> kw = gridMemberService.list(admin.getAdminId(), null, "业务测试-m1");
        assertTrue(kw.stream().anyMatch(v -> v.getUserId().equals(u1.getId())));
        // 权限
        assertThrows(BizException.class, () -> gridMemberService.list(null, null, null));
    }

    // ---------- 3. 审核 -> 派单 -> 任务 ----------

    @Test
    void approveThenAssignCreatesTask() {
        Admins admin = insertAdmin("flow");
        Users submitter = insertUser("submit", 1);
        Users gridUser = insertUser("griduser", 1);
        Grids grid = insertGrid("flow", 1);
        SupervisionEvent e = createEvent(submitter, "派单流转事件");

        // 审核通过
        SupervisionEvent approved = supervisionEventService.approve(e.getId(), admin.getAdminId(), "属实");
        assertEquals("APPROVED", approved.getStatus());

        // 未审核状态不可派单
        SupervisionEvent e2 = createEvent(submitter, "未审核不可派单");
        AssignEventDTO dto = new AssignEventDTO();
        dto.setGridId(grid.getId());
        dto.setAssigneeId(gridUser.getId());
        dto.setPriority("HIGH");
        assertThrows(BizException.class, () -> supervisionEventService.assign(e2.getId(), admin.getAdminId(), dto));

        // 派单
        dto.setPriority("HIGH");
        dto.setRemark("立即处理");
        SupervisionEvent assigned = supervisionEventService.assign(e.getId(), admin.getAdminId(), dto);
        assertEquals("ASSIGNED", assigned.getStatus());
        assertEquals(gridUser.getId(), assigned.getAssigneeId());
        // 关联任务已创建
        List<InspectionTask> tasks = inspectionTaskDao.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<InspectionTask>().eq("event_id", e.getId()));
        assertEquals(1, tasks.size());
        assertEquals("ASSIGNED", tasks.get(0).getStatus());
        assertEquals("HIGH", tasks.get(0).getPriority());
        assertEquals(gridUser.getId(), tasks.get(0).getAssigneeId());
        // 状态日志 3 条：创建/审核/派单
        Integer logs = eventStatusLogDao.selectCount(
                new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<org.nep.nepsystem.bean.EventStatusLog>().eq("event_id", e.getId()));
        assertEquals(3, logs);
        // 重复派单拒绝（已 ASSIGNED）
        assertThrows(BizException.class, () -> supervisionEventService.assign(e.getId(), admin.getAdminId(), dto));
    }

    @Test
    void taskCreatePageAndStats() {
        Admins admin = insertAdmin("task");
        Users g1 = insertUser("t1", 1);
        Users g2 = insertUser("t2", 1);
        Grids grid = insertGrid("task", 1);
        // 独立任务创建
        InspectionTaskCreateDTO dto = new InspectionTaskCreateDTO();
        dto.setGridId(grid.getId());
        dto.setAssigneeId(g1.getId());
        dto.setPriority("LOW");
        dto.setDeadline(new Date(System.currentTimeMillis() + 86400000L));
        dto.setDescription("例行巡检");
        InspectionTask t1 = inspectionTaskService.create(admin.getAdminId(), dto);
        assertNotNull(t1.getId());
        assertTrue(t1.getTaskNo().startsWith("TK"));
        // 非法优先级拒绝
        InspectionTaskCreateDTO bad = new InspectionTaskCreateDTO();
        bad.setAssigneeId(g1.getId());
        bad.setPriority("URGENT");
        assertThrows(BizException.class, () -> inspectionTaskService.create(admin.getAdminId(), bad));
        // 无网格员拒绝
        InspectionTaskCreateDTO noOne = new InspectionTaskCreateDTO();
        assertThrows(BizException.class, () -> inspectionTaskService.create(admin.getAdminId(), noOne));
        // 第二任务给 g2
        InspectionTaskCreateDTO dto2 = new InspectionTaskCreateDTO();
        dto2.setAssigneeId(g2.getId());
        inspectionTaskService.create(admin.getAdminId(), dto2);
        // 任务分页
        PageResult<TaskVO> page = inspectionTaskService.page(admin.getAdminId(), 1, 10, null, null, null, null);
        assertTrue(page.getTotal() >= 2);
        // 按网格员过滤
        PageResult<TaskVO> byAssignee = inspectionTaskService.page(admin.getAdminId(), 1, 10, null, g1.getId(), null, null);
        assertTrue(byAssignee.getTotal() >= 1);
        assertTrue(byAssignee.getRecords().stream().allMatch(v -> v.getAssigneeId().equals(g1.getId())));
        // 完成率统计（暂无 CLOSED -> 0%）
        List<AssigneeStatsVO> stats = inspectionTaskService.assigneeStats(admin.getAdminId());
        assertTrue(stats.stream().anyMatch(v -> v.getUserId().equals(g1.getId()) && v.getTotalTasks() >= 1));
        assertEquals(0, stats.stream().filter(v -> v.getUserId().equals(g1.getId())).findFirst().get().getCompletionRate());
        // 权限
        assertThrows(BizException.class, () -> inspectionTaskService.page(null, 1, 10, null, null, null, null));
    }

    // ---------- 4. 工作台统计 ----------

    @Test
    void workbenchStats() {
        Admins admin = insertAdmin("wb");
        Users u = insertUser("wb", 1);
        Users gridUser = insertUser("wb-g", 1);
        Grids grid = insertGrid("wb", 1);
        SupervisionEvent e1 = createEvent(u, "工作台-待审核");
        SupervisionEvent e2 = createEvent(u, "工作台-已通过");
        supervisionEventService.approve(e2.getId(), admin.getAdminId(), null);
        WorkbenchStatsDTO stats = supervisionEventService.adminStats(admin.getAdminId());
        assertTrue(stats.getTodayEvents() >= 2, "今日事件至少2");
        assertTrue(stats.getPendingReview() >= 1, "待审核至少1");
        assertTrue(stats.getPendingAssign() >= 1, "待派单至少1(已审核未派单)");
        // 待处理列表
        List<org.nep.nepsystem.dto.SupervisionEventVO> pending = supervisionEventService.pendingEvents(admin.getAdminId(), 10);
        assertTrue(pending.size() >= 2);
        assertTrue(pending.stream().anyMatch(v -> v.getTitle().contains("待审核")));
        // 派单后 processing+1
        AssignEventDTO dto = new AssignEventDTO();
        dto.setGridId(grid.getId());
        dto.setAssigneeId(gridUser.getId());
        supervisionEventService.assign(e2.getId(), admin.getAdminId(), dto);
        WorkbenchStatsDTO after = supervisionEventService.adminStats(admin.getAdminId());
        assertTrue(after.getProcessing() >= 1, "派单后处理中至少1");
        assertTrue(after.getPendingAssign() >= 0);
        // 权限
        assertThrows(BizException.class, () -> supervisionEventService.adminStats(null));
    }

    // ---------- 5. HTTP 接口层 ----------

    @Test
    void adminEndpointsWork() throws Exception {
        Admins admin = insertAdmin("http");
        Users u = insertUser("http", 1);
        Users gridUser = insertUser("http-g", 1);
        Grids grid = insertGrid("http", 1);

        // 网格接口（无身份 401/403）
        mockMvc.perform(get("/api/grids/list"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(403));
        mockMvc.perform(get("/api/grids/list").header("X-Admin-Id", String.valueOf(admin.getAdminId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        // 网格员分配
        String assignBody = "{\"gridId\":" + grid.getId() + ",\"userId\":" + gridUser.getId() + ",\"role\":\"GRID_USER\"}";
        mockMvc.perform(post("/api/grid-members/assign")
                        .header("X-Admin-Id", String.valueOf(admin.getAdminId()))
                        .contentType(MediaType.APPLICATION_JSON).content(assignBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        // 事件：创建->审核->派单（真实闭环）
        String evBody = "{\"eventType\":\"NOISE\",\"title\":\"HTTP管理端事件\",\"description\":\"desc\",\"level\":\"WARN\"}";
        String resp = mockMvc.perform(post("/api/supervision")
                        .header("X-User-Id", String.valueOf(u.getId()))
                        .contentType(MediaType.APPLICATION_JSON).content(evBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andReturn().getResponse().getContentAsString();
        String eid = new com.fasterxml.jackson.databind.ObjectMapper().readTree(resp).path("data").path("id").asText();

        // 审核
        mockMvc.perform(put("/api/supervision/" + eid + "/approve")
                        .header("X-Admin-Id", String.valueOf(admin.getAdminId()))
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("APPROVED"));

        // 派单
        String assignEventBody = "{\"gridId\":" + grid.getId() + ",\"assigneeId\":" + gridUser.getId() + ",\"priority\":\"MEDIUM\",\"remark\":\"请尽快处理\"}";
        mockMvc.perform(put("/api/supervision/" + eid + "/assign")
                        .header("X-Admin-Id", String.valueOf(admin.getAdminId()))
                        .contentType(MediaType.APPLICATION_JSON).content(assignEventBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("ASSIGNED"));

        // 工作台统计 + 待处理
        mockMvc.perform(get("/api/supervision/admin/stats").header("X-Admin-Id", String.valueOf(admin.getAdminId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
        mockMvc.perform(get("/api/supervision/admin/pending").header("X-Admin-Id", String.valueOf(admin.getAdminId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        // 任务列表 + 完成率
        mockMvc.perform(get("/api/tasks/page").header("X-Admin-Id", String.valueOf(admin.getAdminId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.total").isNumber());
        mockMvc.perform(get("/api/tasks/assignee-stats").header("X-Admin-Id", String.valueOf(admin.getAdminId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }
}
