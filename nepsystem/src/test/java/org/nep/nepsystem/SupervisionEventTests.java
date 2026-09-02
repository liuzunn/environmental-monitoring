package org.nep.nepsystem;

import org.junit.jupiter.api.Test;
import org.nep.nepsystem.bean.Admins;
import org.nep.nepsystem.bean.Devices;
import org.nep.nepsystem.bean.EventStatusLog;
import org.nep.nepsystem.bean.Regions;
import org.nep.nepsystem.bean.SupervisionAttachment;
import org.nep.nepsystem.bean.SupervisionEvent;
import org.nep.nepsystem.bean.Users;
import org.nep.nepsystem.common.PageResult;
import org.nep.nepsystem.dao.AdminsDao;
import org.nep.nepsystem.dao.DevicesDao;
import org.nep.nepsystem.dao.EventStatusLogDao;
import org.nep.nepsystem.dao.RegionsDao;
import org.nep.nepsystem.dao.SupervisionAttachmentDao;
import org.nep.nepsystem.dao.SupervisionEventDao;
import org.nep.nepsystem.dao.UsersDao;
import org.nep.nepsystem.dto.SupervisionAttachmentDTO;
import org.nep.nepsystem.dto.SupervisionCreateDTO;
import org.nep.nepsystem.dto.SupervisionDetailVO;
import org.nep.nepsystem.dto.SupervisionEventVO;
import org.nep.nepsystem.exception.BizException;
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
 * 监督事件业务测试（Phase 2 新增）：
 * 覆盖 公众创建/公众查看/管理员查看/审核通过/驳回/越权拒绝/状态机与日志落库。
 */
@SpringBootTest(properties = {"simulator.enabled=false", "quality.scan.enabled=false", "alert.auto-resolve-hold-ms=0"})
@Transactional
@AutoConfigureMockMvc
class SupervisionEventTests {

    @Autowired private SupervisionEventService supervisionEventService;
    @Autowired private UsersDao usersDao;
    @Autowired private AdminsDao adminsDao;
    @Autowired private DevicesDao devicesDao;
    @Autowired private RegionsDao regionsDao;
    @Autowired private SupervisionEventDao supervisionEventDao;
    @Autowired private SupervisionAttachmentDao supervisionAttachmentDao;
    @Autowired private EventStatusLogDao eventStatusLogDao;
    @Autowired private MockMvc mockMvc;

    private String uid = String.valueOf(System.nanoTime());

    private Users insertUser(String tag) {
        Users u = new Users();
        u.setUsername("e2e-sup-" + tag + "-" + uid);
        u.setPassword("123456");
        u.setNickname("监督测试用户-" + tag);
        u.setRole("USER");
        u.setStatus(1);
        u.setCreateTime(new Date());
        usersDao.insert(u);
        return u;
    }

    private Admins insertAdmin(String tag) {
        Admins a = new Admins();
        a.setAdminCode("e2e-admin-" + tag + "-" + uid);
        a.setPassword("123456");
        a.setRemarks("监督测试管理员-" + tag);
        adminsDao.insert(a);
        return a;
    }

    private Devices insertDevice(String tag) {
        Devices d = new Devices();
        d.setDeviceCode("E2E-SUP-" + tag + "-" + uid);
        d.setDeviceName("监督测试设备-" + tag);
        d.setType("AIR");
        d.setLocation("测试位置");
        d.setStatus(1);
        devicesDao.insert(d);
        return d;
    }

    private Regions insertRegion(String tag) {
        Regions r = new Regions();
        r.setName("监督测试区域-" + tag + "-" + uid);
        r.setParentId(0);
        r.setDescription("测试区域");
        regionsDao.insert(r);
        return r;
    }

    private SupervisionCreateDTO buildDTO(String title) {
        SupervisionCreateDTO dto = new SupervisionCreateDTO();
        dto.setEventType("POLLUTION");
        dto.setTitle(title);
        dto.setDescription("测试事件描述");
        dto.setLocation("测试位置描述");
        dto.setLevel("WARN");
        return dto;
    }

    // ---------- 1. 公众创建 ----------

    @Test
    void publicUserCreatesEvent() {
        Users u = insertUser("create");
        SupervisionCreateDTO dto = buildDTO("创建测试事件");
        Devices d = insertDevice("create");
        Regions r = insertRegion("create");
        dto.setDeviceId(d.getId());
        dto.setRegionId(r.getId());
        dto.setLat(new BigDecimal("31.2304000"));
        dto.setLng(new BigDecimal("121.4737000"));
        SupervisionAttachmentDTO att = new SupervisionAttachmentDTO();
        att.setFileName("photo1.jpg");
        att.setFilePath("/upload/photo1.jpg");
        att.setFileSize(1024L);
        att.setContentType("image/jpeg");
        dto.setAttachments(Collections.singletonList(att));

        SupervisionEvent e = supervisionEventService.create(u.getId(), dto);

        assertNotNull(e.getId());
        assertNotNull(e.getEventNo());
        assertTrue(e.getEventNo().startsWith("EV"));
        assertEquals("PENDING_REVIEW", e.getStatus(), "创建后状态必须为待审核");
        assertEquals(u.getId(), e.getUserId());
        // 附件落库
        List<SupervisionAttachment> atts = supervisionAttachmentDao.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<SupervisionAttachment>().eq("event_id", e.getId()));
        assertEquals(1, atts.size());
        assertEquals("photo1.jpg", atts.get(0).getFileName());
        // 状态日志落库：NULL -> PENDING_REVIEW
        List<EventStatusLog> logs = eventStatusLogDao.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<EventStatusLog>().eq("event_id", e.getId()));
        assertEquals(1, logs.size());
        assertNull(logs.get(0).getFromStatus());
        assertEquals("PENDING_REVIEW", logs.get(0).getToStatus());
        assertEquals(u.getId(), logs.get(0).getOperatorId());
    }

    @Test
    void createRequiresValidUser() {
        assertThrows(BizException.class, () -> supervisionEventService.create(null, buildDTO("无身份")));
        assertThrows(BizException.class, () -> supervisionEventService.create(99999999, buildDTO("用户不存在")));
        Users disabled = insertUser("disabled");
        disabled.setStatus(0);
        usersDao.updateById(disabled);
        assertThrows(BizException.class, () -> supervisionEventService.create(disabled.getId(), buildDTO("禁用用户")));
    }

    @Test
    void createValidatesPayload() {
        Users u = insertUser("payload");
        SupervisionCreateDTO noType = buildDTO("缺类型");
        noType.setEventType(null);
        assertThrows(BizException.class, () -> supervisionEventService.create(u.getId(), noType));
        SupervisionCreateDTO badType = buildDTO("非法类型");
        badType.setEventType("HACK");
        assertThrows(BizException.class, () -> supervisionEventService.create(u.getId(), badType));
        SupervisionCreateDTO noTitle = buildDTO("缺标题");
        noTitle.setTitle(null);
        assertThrows(BizException.class, () -> supervisionEventService.create(u.getId(), noTitle));
        // 不存在的设备/区域
        SupervisionCreateDTO badDev = buildDTO("坏设备");
        badDev.setDeviceId(99999999);
        assertThrows(BizException.class, () -> supervisionEventService.create(u.getId(), badDev));
        SupervisionCreateDTO badRegion = buildDTO("坏区域");
        badRegion.setRegionId(99999999);
        assertThrows(BizException.class, () -> supervisionEventService.create(u.getId(), badRegion));
    }

    // ---------- 2. 公众查看自己的 ----------

    @Test
    void publicUserSeesOnlyOwnEvents() {
        Users a = insertUser("mine-a");
        Users b = insertUser("mine-b");
        supervisionEventService.create(a.getId(), buildDTO("A的事件1"));
        supervisionEventService.create(a.getId(), buildDTO("A的事件2"));
        supervisionEventService.create(b.getId(), buildDTO("B的事件"));

        PageResult<SupervisionEventVO> mineA = supervisionEventService.pageMine(a.getId(), 1, 10, null);
        assertTrue(mineA.getTotal() >= 2);
        assertTrue(mineA.getRecords().stream().allMatch(v -> v.getSubmitterId().equals(a.getId())), "只能看到自己的事件");
        assertTrue(mineA.getRecords().stream().noneMatch(v -> v.getTitle().contains("B的事件")));
        // 状态过滤
        PageResult<SupervisionEventVO> pendingA = supervisionEventService.pageMine(a.getId(), 1, 10, "PENDING_REVIEW");
        assertEquals(mineA.getTotal(), pendingA.getTotal());
        PageResult<SupervisionEventVO> approvedA = supervisionEventService.pageMine(a.getId(), 1, 10, "APPROVED");
        assertEquals(0, approvedA.getTotal());
    }

    // ---------- 3. 详情（本人/管理员/越权） ----------

    @Test
    void detailAccessControl() {
        Users owner = insertUser("owner");
        Users other = insertUser("other");
        Admins admin = insertAdmin("detail");
        SupervisionEvent e = supervisionEventService.create(owner.getId(), buildDTO("详情权限事件"));

        // 本人可看
        SupervisionDetailVO mine = supervisionEventService.detail(e.getId(), owner.getId(), null);
        assertNotNull(mine.getEvent());
        assertEquals(1, mine.getStatusLogs().size());
        assertEquals("PENDING_REVIEW", mine.getStatusLogs().get(0).getToStatus());
        // 管理员可看（无需是本人）
        SupervisionDetailVO byAdmin = supervisionEventService.detail(e.getId(), null, admin.getAdminId());
        assertNotNull(byAdmin.getEvent());
        // 他人不可看
        assertThrows(BizException.class, () -> supervisionEventService.detail(e.getId(), other.getId(), null));
        // 无任何身份不可看
        assertThrows(BizException.class, () -> supervisionEventService.detail(e.getId(), null, null));
        // 无效管理员不可放行
        assertThrows(BizException.class, () -> supervisionEventService.detail(e.getId(), other.getId(), 99999999));
        // 事件不存在
        assertThrows(BizException.class, () -> supervisionEventService.detail(99999999L, owner.getId(), null));
    }

    // ---------- 4. 管理员查看全部 ----------

    @Test
    void adminSeesAllEvents() {
        Users a = insertUser("all-a");
        Users b = insertUser("all-b");
        Admins admin = insertAdmin("all");
        supervisionEventService.create(a.getId(), buildDTO("管理员视角-A"));
        supervisionEventService.create(b.getId(), buildDTO("管理员视角-B"));

        PageResult<SupervisionEventVO> all = supervisionEventService.pageAll(admin.getAdminId(), 1, 10, null, null);
        assertTrue(all.getTotal() >= 2);
        assertTrue(all.getRecords().stream().anyMatch(v -> v.getTitle().contains("管理员视角-A")));
        assertTrue(all.getRecords().stream().anyMatch(v -> v.getTitle().contains("管理员视角-B")));
        // 关键字过滤
        PageResult<SupervisionEventVO> kw = supervisionEventService.pageAll(admin.getAdminId(), 1, 10, null, "管理员视角-A");
        assertEquals(1, kw.getTotal());
        // 非管理员拒绝
        assertThrows(BizException.class, () -> supervisionEventService.pageAll(null, 1, 10, null, null));
        assertThrows(BizException.class, () -> supervisionEventService.pageAll(99999999, 1, 10, null, null));
    }

    // ---------- 5. 审核通过 / 6. 驳回 ----------

    @Test
    void adminApprovesEvent() {
        Users u = insertUser("approve");
        Admins admin = insertAdmin("approve");
        SupervisionEvent e = supervisionEventService.create(u.getId(), buildDTO("审核通过事件"));

        SupervisionEvent approved = supervisionEventService.approve(e.getId(), admin.getAdminId(), "情况属实");
        assertEquals("APPROVED", approved.getStatus());

        List<EventStatusLog> logs = eventStatusLogDao.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<EventStatusLog>()
                        .eq("event_id", e.getId()).orderByAsc("id"));
        assertEquals(2, logs.size(), "创建 + 审核共 2 条日志");
        assertEquals("PENDING_REVIEW", logs.get(0).getToStatus());
        assertEquals("PENDING_REVIEW", logs.get(1).getFromStatus());
        assertEquals("APPROVED", logs.get(1).getToStatus());
        assertTrue(logs.get(1).getRemark().contains("审核通过"), "日志备注应含审核信息: " + logs.get(1).getRemark());
    }

    @Test
    void adminRejectsEvent() {
        Users u = insertUser("reject");
        Admins admin = insertAdmin("reject");
        SupervisionEvent e = supervisionEventService.create(u.getId(), buildDTO("驳回事件"));

        SupervisionEvent rejected = supervisionEventService.reject(e.getId(), admin.getAdminId(), "证据不足");
        assertEquals("REJECTED", rejected.getStatus());

        List<EventStatusLog> logs = eventStatusLogDao.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<EventStatusLog>()
                        .eq("event_id", e.getId()).orderByAsc("id"));
        assertEquals(2, logs.size());
        assertEquals("REJECTED", logs.get(1).getToStatus());
        assertTrue(logs.get(1).getRemark().contains("驳回"), "日志备注应含驳回信息");
    }

    @Test
    void reviewRequiresAdminAndPendingState() {
        Users u = insertUser("review-perm");
        Users fakeAdmin = insertUser("fake-admin"); // 普通用户冒充管理员
        Admins admin = insertAdmin("review-perm");
        SupervisionEvent e = supervisionEventService.create(u.getId(), buildDTO("审核权限事件"));

        // 非管理员（普通用户id）审核拒绝
        assertThrows(BizException.class, () -> supervisionEventService.approve(e.getId(), fakeAdmin.getId(), null));
        // 无身份审核拒绝
        assertThrows(BizException.class, () -> supervisionEventService.approve(e.getId(), null, null));
        // 审核通过后，再次审核（重复审核/非法状态转换）拒绝
        supervisionEventService.approve(e.getId(), admin.getAdminId(), null);
        assertThrows(BizException.class, () -> supervisionEventService.approve(e.getId(), admin.getAdminId(), null));
        assertThrows(BizException.class, () -> supervisionEventService.reject(e.getId(), admin.getAdminId(), null));
        // 驳回后不可再操作
        SupervisionEvent e2 = supervisionEventService.create(u.getId(), buildDTO("驳回后冻结"));
        supervisionEventService.reject(e2.getId(), admin.getAdminId(), null);
        assertThrows(BizException.class, () -> supervisionEventService.approve(e2.getId(), admin.getAdminId(), null));
        // 事件不存在
        assertThrows(BizException.class, () -> supervisionEventService.approve(99999999L, admin.getAdminId(), null));
    }

    // ---------- 7. HTTP 接口层 ----------

    @Test
    void endpointsWork() throws Exception {
        Users u = insertUser("http");
        Admins admin = insertAdmin("http");
        Devices d = insertDevice("http");

        // 创建
        String body = "{\"eventType\":\"NOISE\",\"title\":\"HTTP接口事件\",\"description\":\"desc\","
                + "\"deviceId\":" + d.getId() + ",\"level\":\"WARN\","
                + "\"attachments\":[{\"fileName\":\"a.png\",\"filePath\":\"/u/a.png\"}]}";
        String resp = mockMvc.perform(post("/api/supervision")
                        .header("X-User-Id", String.valueOf(u.getId()))
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.status").value("PENDING_REVIEW"))
                .andReturn().getResponse().getContentAsString();
        String eventId = new com.fasterxml.jackson.databind.ObjectMapper()
                .readTree(resp).path("data").path("id").asText();
        assertTrue(eventId.matches("[0-9]+"), "应解析出数字事件ID: " + eventId);

        // 无身份创建 -> 401
        mockMvc.perform(post("/api/supervision").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(401));

        // 我的列表
        mockMvc.perform(get("/api/supervision/mine").header("X-User-Id", String.valueOf(u.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.total").isNumber());

        // 详情（本人）
        mockMvc.perform(get("/api/supervision/" + eventId).header("X-User-Id", String.valueOf(u.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.event.status").value("PENDING_REVIEW"))
                .andExpect(jsonPath("$.data.attachments.length()").value(1))
                .andExpect(jsonPath("$.data.statusLogs.length()").value(1));

        // 他人详情 -> 403
        Users other = insertUser("http-other");
        mockMvc.perform(get("/api/supervision/" + eventId).header("X-User-Id", String.valueOf(other.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(403));

        // 管理员列表（无身份 -> 401；有效管理员 -> 200）
        mockMvc.perform(get("/api/supervision/admin/list"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(401));
        mockMvc.perform(get("/api/supervision/admin/list").header("X-Admin-Id", String.valueOf(admin.getAdminId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        // 审核通过
        mockMvc.perform(put("/api/supervision/" + eventId + "/approve")
                        .header("X-Admin-Id", String.valueOf(admin.getAdminId()))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"remark\":\"ok\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("APPROVED"));

        // 非管理员审核（普通用户id冒充X-Admin-Id）-> 403
        SupervisionEvent e2 = supervisionEventService.create(u.getId(), buildDTO("HTTP越权审核"));
        mockMvc.perform(put("/api/supervision/" + e2.getId() + "/approve")
                        .header("X-Admin-Id", String.valueOf(other.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(403));
        // 完全无管理员身份 -> 401
        mockMvc.perform(put("/api/supervision/" + e2.getId() + "/approve"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(401));

        // 驳回
        mockMvc.perform(put("/api/supervision/" + e2.getId() + "/reject")
                        .header("X-Admin-Id", String.valueOf(admin.getAdminId()))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"remark\":\"证据不足\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("REJECTED"));
    }
}
