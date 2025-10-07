package com.meeting.assistant.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.meeting.assistant.ai.AIService;
import com.meeting.assistant.entity.Meeting;
import com.meeting.assistant.entity.Transcript;
import com.meeting.assistant.service.MeetingService;
import com.meeting.assistant.service.TranscriptionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.BinaryMessage;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.BinaryWebSocketHandler;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
public class AudioStreamHandler extends BinaryWebSocketHandler {

    private final AIService aiService;
    private final TranscriptionService transcriptionService;
    private final MeetingService meetingService;
    private final ObjectMapper objectMapper;
    private final Map<String, Long> sessionMeetingMap = new ConcurrentHashMap<>();

    public AudioStreamHandler(AIService aiService,
                            TranscriptionService transcriptionService,
                            MeetingService meetingService,
                            ObjectMapper objectMapper) {
        this.aiService = aiService;
        this.transcriptionService = transcriptionService;
        this.meetingService = meetingService;
        this.objectMapper = objectMapper;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        log.info("WebSocket connection established: {}", session.getId());

        // 从URL参数获取meetingId，如果没有则创建新会议
        String meetingIdParam = getQueryParam(session, "meetingId");
        Long meetingId;

        if (meetingIdParam != null) {
            meetingId = Long.parseLong(meetingIdParam);
        } else {
            // 创建新会议
            Meeting meeting = meetingService.createMeeting("新会议 " + LocalDateTime.now());
            meetingId = meeting.getId();
        }

        sessionMeetingMap.put(session.getId(), meetingId);
        log.info("Session {} associated with meeting {}", session.getId(), meetingId);

        // 发送确认消息
        Map<String, Object> response = Map.of(
            "type", "connected",
            "meetingId", meetingId,
            "message", "WebSocket连接成功"
        );
        session.sendMessage(new TextMessage(objectMapper.writeValueAsString(response)));
    }

    @Override
    protected void handleBinaryMessage(WebSocketSession session, BinaryMessage message) {
        byte[] audioData = message.getPayload().array();
        Long meetingId = sessionMeetingMap.get(session.getId());

        if (meetingId == null) {
            log.error("No meeting associated with session {}", session.getId());
            return;
        }

        log.info("Received audio data from session {}, size: {} bytes", session.getId(), audioData.length);

        // 异步处理音频转录
        CompletableFuture.runAsync(() -> {
            try {
                // 调用AI转录
                String text = aiService.transcribe(audioData);

                // 保存转录结果
                Transcript transcript = transcriptionService.saveTranscript(
                    meetingId,
                    text,
                    LocalDateTime.now()
                );

                // 构建响应
                Map<String, Object> response = Map.of(
                    "type", "transcript",
                    "id", transcript.getId(),
                    "content", text,
                    "timestamp", transcript.getTimestamp().toString(),
                    "speakerId", transcript.getSpeaker() != null ? transcript.getSpeaker().getId() : null
                );

                // 推送给客户端
                session.sendMessage(new TextMessage(objectMapper.writeValueAsString(response)));
                log.info("Transcript sent to session {}", session.getId());

            } catch (Exception e) {
                log.error("Error processing audio", e);
                try {
                    Map<String, Object> errorResponse = Map.of(
                        "type", "error",
                        "message", "转录失败: " + e.getMessage()
                    );
                    session.sendMessage(new TextMessage(objectMapper.writeValueAsString(errorResponse)));
                } catch (Exception ex) {
                    log.error("Error sending error message", ex);
                }
            }
        });
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        log.info("WebSocket connection closed: {}, status: {}", session.getId(), status);
        sessionMeetingMap.remove(session.getId());
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) {
        log.error("WebSocket transport error for session {}", session.getId(), exception);
    }

    private String getQueryParam(WebSocketSession session, String paramName) {
        String query = session.getUri().getQuery();
        if (query == null) return null;

        for (String param : query.split("&")) {
            String[] pair = param.split("=");
            if (pair.length == 2 && pair[0].equals(paramName)) {
                return pair[1];
            }
        }
        return null;
    }
}
