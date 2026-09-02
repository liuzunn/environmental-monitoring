package org.nep.nepsystem.ws;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 通知 WebSocket 处理器（Phase 7 扩展，复用现有 /ws/notify 单端点，不新增第二套）：
 * - 原有 broadcast() 全量广播保持不变（NEPV 实时数据/告警链路零影响）
 * - 新增身份订阅：连接时携带 query 参数  role=ADMIN|GRID|PUBLIC & id=xxx
 *   （ADMIN: admins.admin_id；GRID/PUBLIC: users.id）
 * - 新增定向通知：sendNotifyToAdmins / sendNotifyToUser
 *   业务通知消息格式: {type:"notify", biz:"...", message:"...", time:"...", ...extra}
 * 通知只做实时提醒；数据仍以 REST API + MySQL 为准。
 */
@Component
public class NotifyWebSocketHandler extends TextWebSocketHandler {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(NotifyWebSocketHandler.class);

    private static final ObjectMapper MAPPER = new ObjectMapper();

    /** 在线会话集合：sessionId -> session（原有） */
    private static final Map<String, WebSocketSession> SESSIONS = new ConcurrentHashMap<>();

    /** 会话身份：sessionId -> {role, id}（Phase 7 新增） */
    private static final Map<String, Identity> IDENTITIES = new ConcurrentHashMap<>();

    /** 会话身份 */
    public static class Identity {
        public final String role;
        public final Integer id;
        public Identity(String role, Integer id) {
            this.role = role;
            this.id = id;
        }
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        String sid = session.getId();
        SESSIONS.put(sid, session);
        // 解析身份：/ws/notify?role=ADMIN&id=1
        Identity identity = parseIdentity(session);
        if (identity != null) {
            IDENTITIES.put(sid, identity);
        }
        log.info("WebSocket 连接建立: {} 身份={} 当前在线 {}", sid,
                identity != null ? identity.role + "/" + identity.id : "anonymous", SESSIONS.size());
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        String sid = session.getId();
        SESSIONS.remove(sid);
        IDENTITIES.remove(sid);
        log.info("WebSocket 连接关闭: {}, 当前在线 {}", sid, SESSIONS.size());
    }

    private Identity parseIdentity(WebSocketSession session) {
        try {
            if (session.getUri() == null || session.getUri().getQuery() == null) {
                return null;
            }
            String query = session.getUri().getQuery();
            String role = null;
            Integer id = null;
            for (String pair : query.split("&")) {
                String[] kv = pair.split("=", 2);
                if (kv.length != 2) continue;
                if ("role".equals(kv[0])) role = kv[1];
                if ("id".equals(kv[0])) {
                    try {
                        id = Integer.valueOf(kv[1]);
                    } catch (NumberFormatException ignored) {
                    }
                }
            }
            if (!StringUtils.hasText(role) || id == null) {
                return null;
            }
            return new Identity(role, id);
        } catch (Exception e) {
            return null;
        }
    }

    // ---------- 原有：全量广播（NEPV 实时数据/告警，保持不变） ----------

    public void broadcast(String json) {
        TextMessage message = new TextMessage(json);
        for (WebSocketSession session : SESSIONS.values()) {
            try {
                if (session.isOpen()) {
                    session.sendMessage(message);
                }
            } catch (IOException e) {
                log.warn("WebSocket 广播失败: {}", e.getMessage());
            }
        }
    }

    // ---------- Phase 7 新增：业务通知（定向） ----------

    /** 组装 notify 消息 JSON（内部使用 Jackson，避免手拼转义问题） */
    private String notifyJson(String biz, String message, Map<String, Object> extra) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("type", "notify");
        payload.put("biz", biz);
        payload.put("message", message);
        payload.put("time", new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()));
        if (extra != null) {
            payload.putAll(extra);
        }
        try {
            return MAPPER.writeValueAsString(payload);
        } catch (Exception e) {
            return "{\"type\":\"notify\",\"biz\":\"" + biz + "\"}";
        }
    }

    private void sendTo(WebSocketSession session, String json) {
        try {
            if (session != null && session.isOpen()) {
                session.sendMessage(new TextMessage(json));
            }
        } catch (IOException e) {
            log.warn("WebSocket 通知发送失败: {}", e.getMessage());
        }
    }

    /** 通知全部管理员（role=ADMIN 的会话） */
    public void sendNotifyToAdmins(String biz, String message, Map<String, Object> extra) {
        String json = notifyJson(biz, message, extra);
        for (Map.Entry<String, WebSocketSession> e : SESSIONS.entrySet()) {
            Identity identity = IDENTITIES.get(e.getKey());
            if (identity != null && "ADMIN".equals(identity.role)) {
                sendTo(e.getValue(), json);
            }
        }
    }

    /** 通知指定用户（role=GRID 或 PUBLIC 的会话，按 users.id 匹配） */
    public void sendNotifyToUser(Integer userId, String biz, String message, Map<String, Object> extra) {
        if (userId == null) {
            return;
        }
        String json = notifyJson(biz, message, extra);
        for (Map.Entry<String, WebSocketSession> e : SESSIONS.entrySet()) {
            Identity identity = IDENTITIES.get(e.getKey());
            if (identity != null && userId.equals(identity.id)
                    && ("GRID".equals(identity.role) || "PUBLIC".equals(identity.role))) {
                sendTo(e.getValue(), json);
            }
        }
    }

    /** 当前在线人数（测试/监控用） */
    public int onlineCount() {
        return SESSIONS.size();
    }
}