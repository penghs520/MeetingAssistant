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

    // 音频缓冲：每个session一个缓冲区
    private final Map<String, AudioBuffer> sessionAudioBuffers = new ConcurrentHashMap<>();

    // 转录结果合并：每个session一个文本缓冲区
    private final Map<String, TranscriptBuffer> sessionTranscriptBuffers = new ConcurrentHashMap<>();

    // 缓冲配置（优化为更长的缓冲，确保完整句子）
    private static final int BUFFER_SIZE_BYTES = 80000; // 约5秒的音频（16kHz, 16bit, mono）
    private static final long BUFFER_TIMEOUT_MS = 4000; // 4秒超时（让 Android 发送 2.5 秒后有缓冲）

    // 转录文本合并配置
    private static final long TRANSCRIPT_MERGE_TIMEOUT_MS = 5000; // 5秒内的转录结果会合并（允许发言人停顿思考）

    // 音频缓冲类
    private static class AudioBuffer {
        private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(AudioBuffer.class);
        private final java.io.ByteArrayOutputStream buffer = new java.io.ByteArrayOutputStream();
        private long lastFlushTime = System.currentTimeMillis();

        public synchronized void append(byte[] data) {
            try {
                buffer.write(data);
            } catch (Exception e) {
                log.error("Error appending to buffer", e);
            }
        }

        public synchronized byte[] getAndClear() {
            byte[] data = buffer.toByteArray();
            buffer.reset();
            lastFlushTime = System.currentTimeMillis();
            return data;
        }

        public synchronized boolean shouldFlush() {
            return buffer.size() >= BUFFER_SIZE_BYTES ||
                   (System.currentTimeMillis() - lastFlushTime) > BUFFER_TIMEOUT_MS;
        }

        public synchronized int size() {
            return buffer.size();
        }
    }

    // 转录文本合并缓冲类
    private static class TranscriptBuffer {
        private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(TranscriptBuffer.class);
        private final StringBuilder textBuffer = new StringBuilder();
        private long lastUpdateTime = System.currentTimeMillis();

        public synchronized void append(String text) {
            if (text != null && !text.trim().isEmpty()) {
                textBuffer.append(text);
                lastUpdateTime = System.currentTimeMillis();
            }
        }

        public synchronized String getAndClear() {
            String text = textBuffer.toString().trim();
            textBuffer.setLength(0);
            lastUpdateTime = System.currentTimeMillis();
            return text;
        }

        public synchronized boolean shouldFlush() {
            String currentText = textBuffer.toString().trim();
            if (currentText.isEmpty()) {
                return false;
            }

            // 优先检查是否以句子结束符号结尾（中英文句号、问号、感叹号）
            boolean endsWithPunctuation = currentText.endsWith("。") ||
                                         currentText.endsWith("！") ||
                                         currentText.endsWith("？") ||
                                         currentText.endsWith(".") ||
                                         currentText.endsWith("!") ||
                                         currentText.endsWith("?");

            // 如果有明确的句子结束符号，立即刷新
            if (endsWithPunctuation) {
                return true;
            }

            // 否则，只有超时才刷新（允许较长的停顿）
            boolean timeout = (System.currentTimeMillis() - lastUpdateTime) > TRANSCRIPT_MERGE_TIMEOUT_MS;
            return timeout;
        }

        public synchronized boolean isEmpty() {
            return textBuffer.length() == 0;
        }
    }

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
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        // 接收base64编码的音频数据
        String base64Audio = message.getPayload();
        byte[] audioData = java.util.Base64.getDecoder().decode(base64Audio);

        processAudioData(session, audioData);
    }

    @Override
    protected void handleBinaryMessage(WebSocketSession session, BinaryMessage message) {
        byte[] audioData = message.getPayload().array();
        processAudioData(session, audioData);
    }

    private void processAudioData(WebSocketSession session, byte[] audioData) {
        Long meetingId = sessionMeetingMap.get(session.getId());

        if (meetingId == null) {
            log.error("No meeting associated with session {}", session.getId());
            return;
        }

        // 获取或创建该session的缓冲区
        AudioBuffer audioBuffer = sessionAudioBuffers.computeIfAbsent(
            session.getId(),
            k -> new AudioBuffer()
        );

        // 将数据添加到缓冲区
        audioBuffer.append(audioData);

        log.debug("Received audio data from session {}, size: {} bytes, buffer total: {} bytes",
            session.getId(), audioData.length, audioBuffer.size());

        // 检查是否应该刷新缓冲区
        if (audioBuffer.shouldFlush()) {
            byte[] bufferedAudio = audioBuffer.getAndClear();
            log.info("Flushing audio buffer for session {}, size: {} bytes", session.getId(), bufferedAudio.length);

            // 异步处理音频转录
            CompletableFuture.runAsync(() -> {
                try {
                    // 调用AI转录
                    String text = aiService.transcribe(bufferedAudio);

                    if (text == null || text.trim().isEmpty()) {
                        log.debug("Empty transcription result, skipping");
                        return;
                    }

                    // 获取或创建转录文本缓冲区
                    TranscriptBuffer transcriptBuffer = sessionTranscriptBuffers.computeIfAbsent(
                        session.getId(),
                        k -> new TranscriptBuffer()
                    );

                    // 将转录结果添加到文本缓冲区
                    transcriptBuffer.append(text);
                    log.info("Appended text to buffer for session {}: {}", session.getId(), text);

                    // 检查是否应该刷新文本缓冲区
                    if (transcriptBuffer.shouldFlush()) {
                        String mergedText = transcriptBuffer.getAndClear();
                        log.info("Flushing transcript buffer for session {}: {}", session.getId(), mergedText);

                        // 保存合并后的转录结果
                        Transcript transcript = transcriptionService.saveTranscript(
                            meetingId,
                            mergedText,
                            LocalDateTime.now()
                        );

                        // 构建响应
                        Map<String, Object> response = new java.util.HashMap<>();
                        response.put("type", "transcript");
                        response.put("id", transcript.getId());
                        response.put("content", mergedText);
                        response.put("timestamp", transcript.getTimestamp().toString());
                        response.put("speakerId", transcript.getSpeaker() != null ? transcript.getSpeaker().getId() : null);

                        // 推送给客户端
                        session.sendMessage(new TextMessage(objectMapper.writeValueAsString(response)));
                        log.info("Merged transcript sent to session {}: {}", session.getId(), mergedText);
                    }

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
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        log.info("WebSocket connection closed: {}, status: {}", session.getId(), status);

        // 发送剩余的转录文本
        TranscriptBuffer transcriptBuffer = sessionTranscriptBuffers.get(session.getId());
        if (transcriptBuffer != null && !transcriptBuffer.isEmpty()) {
            try {
                String remainingText = transcriptBuffer.getAndClear();
                Long meetingId = sessionMeetingMap.get(session.getId());
                if (meetingId != null && !remainingText.isEmpty()) {
                    log.info("Flushing remaining transcript on disconnect: {}", remainingText);
                    transcriptionService.saveTranscript(meetingId, remainingText, LocalDateTime.now());
                }
            } catch (Exception e) {
                log.error("Error flushing remaining transcript", e);
            }
        }

        // 清理缓冲区
        sessionAudioBuffers.remove(session.getId());
        sessionTranscriptBuffers.remove(session.getId());
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
