package org.nep.nepsystem.ws;

import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 通知 WebSocket 处理器：管理所有前端连接，广播实时数据与告警
 */
@Component
public class NotifyWebSocketHandler extends TextWebSocketHandler {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(NotifyWebSocketHandler.class);

    /** 在线会话集合：sessionId -> session */
    private static final Map<String, WebSocketSession> SESSIONS = new ConcurrentHashMap<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        SESSIONS.put(session.getId(), session);
        log.info("WebSocket 连接建立: {}, 当前在线 {}", session.getId(), SESSIONS.size());
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        SESSIONS.remove(session.getId());
        log.info("WebSocket 连接关闭: {}, 当前在线 {}", session.getId(), SESSIONS.size());
    }

    /**
     * 向所有在线客户端广播 JSON 消息
     * @param json 消息内容（JSON 字符串）
     */
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
}