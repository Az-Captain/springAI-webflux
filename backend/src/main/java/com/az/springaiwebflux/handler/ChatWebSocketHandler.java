package com.az.springaiwebflux.handler;

import com.az.springaiwebflux.service.DeepSeekService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;

/**
 * @author Az
 * @date 2025/4/24
 */
@Component
@RequiredArgsConstructor
public class ChatWebSocketHandler extends TextWebSocketHandler {
    private final DeepSeekService deepSeekService;
    private static final Logger log = LoggerFactory.getLogger(ChatWebSocketHandler.class);

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        String question = message.getPayload();
        deepSeekService.handleChatWebSocket(question)
                .subscribe(
                        response -> {
                            try {
                                if (session.isOpen()) {
                                    session.sendMessage(new TextMessage(response));
                                }
                            } catch (IOException e) {
                                log.error("发送消息失败：{}", e.getMessage());
                            }
                        }, error -> {
                            try {
                                if (session.isOpen()) {
                                    session.sendMessage(new TextMessage("Error: " + error.getMessage()));
                                }
                            } catch (IOException e) {
                                log.error("发送错误消息失败:{}", e.getMessage());
                            }
                        });
        super.handleTextMessage(session, message);

    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        log.info("WebSocket 连接已建立:{}", session.getId());
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        log.info("WebSocket 连接已关闭:{},状态：{}", session.getId(), status);
    }
}
