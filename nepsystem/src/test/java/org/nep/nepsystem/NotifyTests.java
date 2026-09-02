package org.nep.nepsystem;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.nep.nepsystem.bean.Admins;
import org.nep.nepsystem.bean.InspectionTask;
import org.nep.nepsystem.bean.SupervisionEvent;
import org.nep.nepsystem.bean.Users;
import org.nep.nepsystem.dao.AdminsDao;
import org.nep.nepsystem.dao.InspectionTaskDao;
import org.nep.nepsystem.dao.UsersDao;
import org.nep.nepsystem.dto.AssignEventDTO;
import org.nep.nepsystem.dto.DetectSubmitDTO;
import org.nep.nepsystem.dto.SupervisionCreateDTO;
import org.nep.nepsystem.service.InspectionTaskService;
import org.nep.nepsystem.service.SupervisionEventService;
import org.nep.nepsystem.ws.NotifyWebSocketHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketExtension;
import org.springframework.web.socket.WebSocketMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.InetSocketAddress;
import java.net.URI;
import java.security.Principal;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Phase 7 WebSocket 业务通知测试（复用现有 /ws/notify，FakeSession 验证定向通知）：
 * 在线通知 / 离线无通知 / 重连 / 重复通知 / 权限隔离
 */
@SpringBootTest(properties = {"simulator.enabled=false", "quality.scan.enabled=false", "alert.auto-resolve-hold-ms=0"})
@Transactional
class NotifyTests {

    @Autowired private AdminsDao adminsDao;
    @Autowired private UsersDao usersDao;
    @Autowired private InspectionTaskDao inspectionTaskDao;
    @Autowired private SupervisionEventService supervisionEventService;
    @Autowired private InspectionTaskService inspectionTaskService;
    @Autowired private NotifyWebSocketHandler handler;

    private final List<FakeSession> opened = new ArrayList<>();
    private static final AtomicInteger SEQ = new AtomicInteger(0);

    /** 最小 Fake WebSocketSession：记录收到的消息文本 */
    static class FakeSession implements WebSocketSession {
        final String id = "fake-" + SEQ.incrementAndGet();
        final URI uri;
        final List<String> received = new ArrayList<>();
        boolean open = true;

        FakeSession(String query) {
            this.uri = query == null ? URI.create("ws://localhost/ws/notify") : URI.create("ws://localhost/ws/notify?" + query);
        }

        @Override public String getId() { return id; }
        @Override public URI getUri() { return uri; }
        @Override public HttpHeaders getHandshakeHeaders() { return new HttpHeaders(); }
        @Override public Map<String, Object> getAttributes() { return new HashMap<>(); }
        @Override public Principal getPrincipal() { return null; }
        @Override public InetSocketAddress getLocalAddress() { return null; }
        @Override public InetSocketAddress getRemoteAddress() { return null; }
        @Override public String getAcceptedProtocol() { return null; }
        @Override public void setTextMessageSizeLimit(int i) { }
        @Override public int getTextMessageSizeLimit() { return 0; }
        @Override public void setBinaryMessageSizeLimit(int i) { }
        @Override public int getBinaryMessageSizeLimit() { return 0; }
        @Override public List<WebSocketExtension> getExtensions() { return new ArrayList<>(); }
        @Override public boolean isOpen() { return open; }
        @Override public void sendMessage(WebSocketMessage<?> message) throws IOException {
            if (message instanceof TextMessage) {
                received.add(((TextMessage) message).getPayload());
            }
        }
        @Override public void close() throws IOException { open = false; }
        @Override public void close(CloseStatus closeStatus) throws IOException { open = false; }
    }

    private FakeSession connect(String query) {
        FakeSession s = new FakeSession(query);
        handler.afterConnectionEstablished(s);
        opened.add(s);
        return s;
    }

    @AfterEach
    void cleanup() {
        for (FakeSession s : opened) {
            try {
                handler.afterConnectionClosed(s, CloseStatus.NORMAL);
            } catch (Exception ignored) {
            }
        }
        opened.clear();
    }

    private Admins admin() {
        Admins a = new Admins();
        a.setAdminCode("e2e-ws-adm-" + System.nanoTime());
        a.setPassword("123456");
        adminsDao.insert(a);
        return a;
    }

    private Users user(String tag) {
        Users u = new Users();
        u.setUsername("e2e-ws-" + tag + "-" + System.nanoTime());
        u.setPassword("123456");
        u.setNickname("通知-" + tag);
        u.setRole("USER");
        u.setStatus(1);
        u.setCreateTime(new Date());
        usersDao.insert(u);
        return u;
    }

    private SupervisionEvent create(Users u, String title) {
        SupervisionCreateDTO dto = new SupervisionCreateDTO();
        dto.setEventType("POLLUTION");
        dto.setTitle(title);
        dto.setDescription("通知测试");
        dto.setLocation("位置");
        dto.setLevel("WARN");
        return supervisionEventService.create(u.getId(), dto);
    }

    private boolean hasBiz(FakeSession s, String biz) {
        return s.received.stream().anyMatch(m -> m.contains("\"biz\":\"" + biz + "\""));
    }

    // ---------- 1. 在线通知：公众提交 -> 管理员 ----------

    @Test
    void adminReceivesSupervisionCreated() {
        FakeSession adminWs = connect("role=ADMIN&id=1");
        Users citizen = user("c1");
        create(citizen, "通知事件A");
        assertTrue(hasBiz(adminWs, "SUPERVISION_CREATED"), "管理员应收到监督创建通知");
        assertTrue(adminWs.received.get(0).contains("type\":\"notify"), "消息类型为 notify");
    }

    // ---------- 2. 派单 -> 网格员（管理员不收到） ----------

    @Test
    void workerReceivesAssignedOnly() {
        Admins ad = admin();
        Users citizen = user("c2");
        Users worker = user("w1");
        FakeSession adminWs = connect("role=ADMIN&id=" + ad.getAdminId());
        FakeSession workerWs = connect("role=GRID&id=" + worker.getId());
        SupervisionEvent e = create(citizen, "派单通知事件");
        supervisionEventService.approve(e.getId(), ad.getAdminId(), null);
        AssignEventDTO dto = new AssignEventDTO();
        dto.setAssigneeId(worker.getId());
        supervisionEventService.assign(e.getId(), ad.getAdminId(), dto);
        assertTrue(hasBiz(workerWs, "TASK_ASSIGNED"), "网格员应收到派单通知");
        assertFalse(hasBiz(adminWs, "TASK_ASSIGNED"), "管理员不应收到 TASK_ASSIGNED（定向给网格员）");
    }

    // ---------- 3. 接单/检测完成 -> 管理员 ----------

    @Test
    void adminReceivesAcceptedAndSubmitted() {
        Admins ad = admin();
        Users citizen = user("c3");
        Users worker = user("w2");
        FakeSession adminWs = connect("role=ADMIN&id=" + ad.getAdminId());
        SupervisionEvent e = create(citizen, "接单通知事件");
        supervisionEventService.approve(e.getId(), ad.getAdminId(), null);
        AssignEventDTO dto = new AssignEventDTO();
        dto.setAssigneeId(worker.getId());
        supervisionEventService.assign(e.getId(), ad.getAdminId(), dto);
        InspectionTask task = inspectionTaskDao.selectOne(
                new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<InspectionTask>().eq("event_id", e.getId()));
        inspectionTaskService.accept(task.getId(), worker.getId());
        assertTrue(hasBiz(adminWs, "TASK_ACCEPTED"), "管理员应收到接单通知");
        inspectionTaskService.start(task.getId(), worker.getId());
        DetectSubmitDTO det = new DetectSubmitDTO();
        det.setPm25(new BigDecimal("66.0"));
        det.setPm10(new BigDecimal("120.0"));
        inspectionTaskService.submit(task.getId(), worker.getId(), det);
        assertTrue(hasBiz(adminWs, "DETECT_SUBMITTED"), "管理员应收到检测完成通知");
        String msg = adminWs.received.stream().filter(m -> m.contains("DETECT_SUBMITTED")).findFirst().orElse("");
        assertTrue(msg.contains("\"aqi\":\"89\""), "通知应含 AQI 值(PM2.5=66->89,PM10=120->85,max=89): " + msg);
    }

    // ---------- 4. 关闭 -> 公众"已处理完成" ----------

    @Test
    void citizenReceivesEventClosed() {
        Admins ad = admin();
        Users citizen = user("c4");
        Users worker = user("w3");
        FakeSession citizenWs = connect("role=PUBLIC&id=" + citizen.getId());
        SupervisionEvent e = create(citizen, "关闭通知事件");
        supervisionEventService.approve(e.getId(), ad.getAdminId(), null);
        AssignEventDTO dto = new AssignEventDTO();
        dto.setAssigneeId(worker.getId());
        supervisionEventService.assign(e.getId(), ad.getAdminId(), dto);
        InspectionTask task = inspectionTaskDao.selectOne(
                new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<InspectionTask>().eq("event_id", e.getId()));
        inspectionTaskService.accept(task.getId(), worker.getId());
        inspectionTaskService.start(task.getId(), worker.getId());
        DetectSubmitDTO det = new DetectSubmitDTO();
        det.setPm25(new BigDecimal("50"));
        inspectionTaskService.submit(task.getId(), worker.getId(), det);
        inspectionTaskService.verify(ad.getAdminId(), task.getId(), null);
        inspectionTaskService.close(ad.getAdminId(), task.getId(), null);
        assertTrue(hasBiz(citizenWs, "EVENT_CLOSED"), "公众应收到事件关闭通知");
        String msg = citizenWs.received.stream().filter(m -> m.contains("EVENT_CLOSED")).findFirst().orElse("");
        assertTrue(msg.contains("已处理完成"), "消息应为已处理完成: " + msg);
    }

    // ---------- 5. 离线无通知 ----------

    @Test
    void offlineNoNotify() {
        Users citizen = user("c5");
        SupervisionEvent e = create(citizen, "离线事件"); // 无任何 session
        // 不应抛异常；handler 在线数 0
        assertEquals(0, handler.onlineCount());
        assertNotNull(e.getId());
    }

    // ---------- 6. 断开后重连可收到 ----------

    @Test
    void reconnectAfterDisconnect() {
        Admins ad = admin();
        FakeSession s1 = connect("role=ADMIN&id=" + ad.getAdminId());
        handler.afterConnectionClosed(s1, CloseStatus.NORMAL);
        opened.remove(s1);
        FakeSession s2 = connect("role=ADMIN&id=" + ad.getAdminId());
        Users citizen = user("c6");
        create(citizen, "重连事件");
        assertFalse(hasBiz(s1, "SUPERVISION_CREATED"), "已断开会话不应收到");
        assertTrue(hasBiz(s2, "SUPERVISION_CREATED"), "重连会话应收到");
    }

    // ---------- 7. 重复通知（两次事件 -> 两条通知） ----------

    @Test
    void multipleNotifies() {
        FakeSession adminWs = connect("role=ADMIN&id=1");
        Users citizen = user("c7");
        create(citizen, "通知事件1");
        create(citizen, "通知事件2");
        long count = adminWs.received.stream().filter(m -> m.contains("SUPERVISION_CREATED")).count();
        assertEquals(2, count, "两次创建应收到两条通知");
    }

    // ---------- 8. 权限隔离 ----------

    @Test
    void identityIsolation() {
        Admins ad = admin();
        Users citizen = user("c8");
        Users workerA = user("wa");
        Users workerB = user("wb");
        FakeSession adminWs = connect("role=ADMIN&id=" + ad.getAdminId());
        FakeSession workerAWs = connect("role=GRID&id=" + workerA.getId());
        FakeSession workerBWs = connect("role=GRID&id=" + workerB.getId());
        FakeSession otherCitizenWs = connect("role=PUBLIC&id=" + workerB.getId()); // 其他公众
        SupervisionEvent e = create(citizen, "隔离事件");
        supervisionEventService.approve(e.getId(), ad.getAdminId(), null);
        AssignEventDTO dto = new AssignEventDTO();
        dto.setAssigneeId(workerA.getId());
        supervisionEventService.assign(e.getId(), ad.getAdminId(), dto);
        // TASK_ASSIGNED 只给 workerA
        assertTrue(hasBiz(workerAWs, "TASK_ASSIGNED"));
        assertFalse(hasBiz(workerBWs, "TASK_ASSIGNED"), "B 网格员不应收到 A 的任务");
        assertFalse(hasBiz(otherCitizenWs, "TASK_ASSIGNED"), "公众不应收到网格员通知");
        assertFalse(hasBiz(adminWs, "TASK_ASSIGNED"), "管理员不应收到定向给网格员的通知");
        // 关闭时 EVENT_CLOSED 只给事件提交人 citizen，不给其他公众 workerB
        InspectionTask task = inspectionTaskDao.selectOne(
                new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<InspectionTask>().eq("event_id", e.getId()));
        inspectionTaskService.accept(task.getId(), workerA.getId());
        inspectionTaskService.start(task.getId(), workerA.getId());
        DetectSubmitDTO det = new DetectSubmitDTO();
        det.setPm25(new BigDecimal("50"));
        inspectionTaskService.submit(task.getId(), workerA.getId(), det);
        inspectionTaskService.verify(ad.getAdminId(), task.getId(), null);
        inspectionTaskService.close(ad.getAdminId(), task.getId(), null);
        assertFalse(hasBiz(otherCitizenWs, "EVENT_CLOSED"), "非提交人的公众不应收到关闭通知（隔离）");
    }
}