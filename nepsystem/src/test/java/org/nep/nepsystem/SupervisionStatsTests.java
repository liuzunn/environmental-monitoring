package org.nep.nepsystem;

import org.junit.jupiter.api.Test;
import org.nep.nepsystem.bean.Admins;
import org.nep.nepsystem.bean.Grids;
import org.nep.nepsystem.bean.InspectionTask;
import org.nep.nepsystem.bean.SupervisionEvent;
import org.nep.nepsystem.bean.Users;
import org.nep.nepsystem.dao.AdminsDao;
import org.nep.nepsystem.dao.GridsDao;
import org.nep.nepsystem.dao.InspectionTaskDao;
import org.nep.nepsystem.dao.UsersDao;
import org.nep.nepsystem.dto.AssignEventDTO;
import org.nep.nepsystem.dto.DetectSubmitDTO;
import org.nep.nepsystem.dto.SupervisionCreateDTO;
import org.nep.nepsystem.service.InspectionTaskService;
import org.nep.nepsystem.service.SupervisionEventService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Phase 8 NEPV 业务数据联动测试：
 * 真实事件全流程后，断言 /api/stats/overview 扩展字段与 /api/stats/supervision 聚合统计随之变化
 */
@SpringBootTest(properties = {"simulator.enabled=false", "quality.scan.enabled=false", "alert.auto-resolve-hold-ms=0"})
@Transactional
@AutoConfigureMockMvc
class SupervisionStatsTests {

    @Autowired private AdminsDao adminsDao;
    @Autowired private UsersDao usersDao;
    @Autowired private GridsDao gridsDao;
    @Autowired private InspectionTaskDao inspectionTaskDao;
    @Autowired private SupervisionEventService supervisionEventService;
    @Autowired private InspectionTaskService inspectionTaskService;
    @Autowired private MockMvc mockMvc;

    private Admins admin() {
        Admins a = new Admins();
        a.setAdminCode("e2e-st-adm-" + System.nanoTime());
        a.setPassword("123456");
        adminsDao.insert(a);
        return a;
    }

    private Users user(String tag) {
        Users u = new Users();
        u.setUsername("e2e-st-" + tag + "-" + System.nanoTime());
        u.setPassword("123456");
        u.setNickname("统计-" + tag);
        u.setRole("USER");
        u.setStatus(1);
        u.setCreateTime(new Date());
        usersDao.insert(u);
        return u;
    }

    private Grids grid() {
        Grids g = new Grids();
        g.setGridCode("GRID-ST-" + System.nanoTime());
        g.setGridName("统计测试网格");
        g.setStatus(1);
        g.setCreateTime(new Date());
        gridsDao.insert(g);
        return g;
    }

    private SupervisionEvent create(Users u, String type, String level) {
        SupervisionCreateDTO dto = new SupervisionCreateDTO();
        dto.setEventType(type);
        dto.setTitle("NEPV联动-" + type + "-" + level);
        dto.setDescription("统计测试");
        dto.setLocation("统计位置");
        dto.setLevel(level);
        return supervisionEventService.create(u.getId(), dto);
    }

    private com.fasterxml.jackson.databind.JsonNode overview() throws Exception {
        String resp = mockMvc.perform(get("/api/stats/overview"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andReturn().getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8);
        return new com.fasterxml.jackson.databind.ObjectMapper().readTree(resp).path("data");
    }

    private com.fasterxml.jackson.databind.JsonNode supervisionStats() throws Exception {
        String resp = mockMvc.perform(get("/api/stats/supervision"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andReturn().getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8);
        return new com.fasterxml.jackson.databind.ObjectMapper().readTree(resp).path("data");
    }

    // ---------- 1. 创建事件后统计变化 ----------

    @Test
    void createEventChangesOverview() throws Exception {
        com.fasterxml.jackson.databind.JsonNode before = overview();
        long beforeToday = before.get("todayEvents").asLong();
        long beforePending = before.get("pendingReview").asLong();

        Users u = user("c1");
        SupervisionEvent e = create(u, "POLLUTION", "WARN");

        com.fasterxml.jackson.databind.JsonNode after = overview();
        assertEquals(beforeToday + 1, after.get("todayEvents").asLong(), "今日监督事件 +1");
        assertEquals(beforePending + 1, after.get("pendingReview").asLong(), "待审核 +1");
        assertTrue(after.get("totalEvents").asLong() >= 1);
    }

    // ---------- 2. 完整闭环后统计（处理中/已完成/处理率） ----------

    @Test
    void closedEventChangesHandleRate() throws Exception {
        Admins ad = admin();
        Users u = user("c2");
        Users w = user("w2");
        Grids g = grid();
        SupervisionEvent e = create(u, "NOISE", "WARN");

        // 审核->派单->接单->开始->提交
        supervisionEventService.approve(e.getId(), ad.getAdminId(), null);
        AssignEventDTO adto = new AssignEventDTO();
        adto.setGridId(g.getId());
        adto.setAssigneeId(w.getId());
        supervisionEventService.assign(e.getId(), ad.getAdminId(), adto);
        InspectionTask task = inspectionTaskDao.selectOne(
                new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<InspectionTask>().eq("event_id", e.getId()));
        inspectionTaskService.accept(task.getId(), w.getId());
        inspectionTaskService.start(task.getId(), w.getId());
        DetectSubmitDTO det = new DetectSubmitDTO();
        det.setPm25(new BigDecimal("50"));
        inspectionTaskService.submit(task.getId(), w.getId(), det);

        com.fasterxml.jackson.databind.JsonNode mid = overview();
        assertTrue(mid.get("processing").asLong() >= 1, "提交后处理中 >=1");

        // 核实+关闭
        inspectionTaskService.verify(ad.getAdminId(), task.getId(), null);
        inspectionTaskService.close(ad.getAdminId(), task.getId(), null);

        com.fasterxml.jackson.databind.JsonNode after = overview();
        long closed = after.get("closedEvents").asLong();
        long total = after.get("totalEvents").asLong();
        assertTrue(closed >= 1, "已完成事件 >=1");
        int rate = after.get("eventHandleRate").asInt();
        assertEquals((int) Math.round(closed * 100.0 / total), rate, "处理率 = 已完成/总数");

        // 聚合统计：类型分布/任务统计/趋势
        com.fasterxml.jackson.databind.JsonNode sup = supervisionStats();
        // 类型分布含 NOISE
        boolean noiseHit = false;
        for (com.fasterxml.jackson.databind.JsonNode n : sup.get("typeDistribution")) {
            if ("NOISE".equals(n.get("eventType").asText())) { noiseHit = true; break; }
        }
        assertTrue(noiseHit, "类型分布应含 NOISE");
        // 任务统计：总数>=1 且关闭>=1
        assertTrue(sup.get("taskStats").get("totalTasks").asLong() >= 1);
        assertTrue(sup.get("taskStats").get("closedTasks").asLong() >= 1);
        // 网格任务含统计测试网格
        boolean gridHit = false;
        for (com.fasterxml.jackson.databind.JsonNode n : sup.get("gridTasks")) {
            if ("统计测试网格".equals(n.get("gridName").asText())) { gridHit = true; assertTrue(n.get("completionRate").asInt() == 100, "该网格任务已全部关闭"); break; }
        }
        assertTrue(gridHit, "网格任务应含统计测试网格");
        // 事件趋势近7天非空
        assertTrue(sup.get("eventTrend").isArray());
    }

    // ---------- 3. 区域分布 / 高风险事件 ----------

    @Test
    void regionAndHighRisk() throws Exception {
        Users u = user("c3");
        // 创建 ALARM 高风险事件（不关闭）
        SupervisionEvent risk = create(u, "POLLUTION", "ALARM");
        com.fasterxml.jackson.databind.JsonNode sup = supervisionStats();
        // 高风险列表含该事件
        boolean riskHit = false;
        for (com.fasterxml.jackson.databind.JsonNode n : sup.get("highRiskEvents")) {
            if (n.get("id").asText().equals(risk.getId().toString())) { riskHit = true; break; }
        }
        assertTrue(riskHit, "高风险事件列表应含 ALARM 未关闭事件");
        // 区域分布数组存在（未关联区域计数 >= 事件数）
        long unbound = 0;
        for (com.fasterxml.jackson.databind.JsonNode n : sup.get("regionDistribution")) {
            if ("未关联区域".equals(n.get("regionName").asText())) unbound = n.get("count").asLong();
        }
        assertTrue(unbound >= 1, "未关联区域事件计数 >=1");
    }

    // ---------- 4. 接口可用性 ----------

    @Test
    void endpointsAvailable() throws Exception {
        mockMvc.perform(get("/api/stats/overview"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.todayEvents").isNumber())
                .andExpect(jsonPath("$.data.eventHandleRate").isNumber());
        mockMvc.perform(get("/api/stats/supervision"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.regionDistribution").isArray())
                .andExpect(jsonPath("$.data.typeDistribution").isArray())
                .andExpect(jsonPath("$.data.taskStats.completionRate").isNumber())
                .andExpect(jsonPath("$.data.highRiskEvents").isArray())
                .andExpect(jsonPath("$.data.eventTrend").isArray());
    }
}