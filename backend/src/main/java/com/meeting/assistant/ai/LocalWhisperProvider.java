package com.meeting.assistant.ai;

import com.meeting.assistant.entity.Speaker;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 本地 Whisper 模型实现
 * 通过 HTTP 调用本地 Python Whisper 服务进行音频转录
 */
@Slf4j
@Service("localWhisperProvider")
public class LocalWhisperProvider implements AIService {

    private final ChatClient chatClient;
    private final RestTemplate restTemplate;

    @Value("${whisper.service.url:http://localhost:5001}")
    private String whisperServiceUrl;

    public LocalWhisperProvider(ChatClient.Builder chatClientBuilder) {
        this.chatClient = chatClientBuilder.build();
        this.restTemplate = new RestTemplate();
    }

    @Override
    public String transcribe(byte[] audioData) {
        log.info("Transcribing audio with local Whisper service, PCM size: {} bytes", audioData.length);
        try {
            // 构建请求头
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);

            // 直接发送 PCM 数据（不需要转换为 WAV，Python 服务会处理）
            HttpEntity<byte[]> requestEntity = new HttpEntity<>(audioData, headers);

            // 调用本地 Whisper 服务
            String transcribeUrl = whisperServiceUrl + "/transcribe";
            ResponseEntity<Map> response = restTemplate.exchange(
                transcribeUrl,
                HttpMethod.POST,
                requestEntity,
                Map.class
            );

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                String text = (String) response.getBody().get("text");
                log.info("Transcription completed, text length: {}", text.length());
                return text;
            } else {
                throw new RuntimeException("Local Whisper service returned unexpected response");
            }
        } catch (Exception e) {
            log.error("Transcription failed", e);
            throw new RuntimeException("本地音频转录失败: " + e.getMessage(), e);
        }
    }

    @Override
    public String summarize(String transcript, List<Speaker> speakers) {
        log.info("Generating meeting summary with GPT (using local Whisper for transcription)");

        String speakerNames = speakers.stream()
            .map(Speaker::getName)
            .collect(Collectors.joining(", "));

        String prompt = buildSummaryPrompt(transcript, speakerNames);

        try {
            String summary = chatClient.prompt()
                .user(prompt)
                .call()
                .content();

            log.info("Summary generated successfully");
            return summary;
        } catch (Exception e) {
            log.error("Summary generation failed", e);
            throw new RuntimeException("总结生成失败: " + e.getMessage(), e);
        }
    }

    @Override
    public String getModelName() {
        return "Local Whisper + GPT-4o";
    }

    private String buildSummaryPrompt(String transcript, String speakerNames) {
        return String.format("""
            请根据以下会议转录内容生成结构化总结，使用简体中文输出：

            参会人员：%s

            转录内容：
            %s

            请按以下格式输出（使用Markdown格式，必须使用简体中文）：

            ## 会议摘要
            （用3-5句话概括整个会议的核心内容）

            ## 关键讨论点
            - 讨论点1
            - 讨论点2
            ...

            ## 决策事项
            - 决策1
            - 决策2
            ...

            ## 待办任务 (Action Items)
            - [ ] 任务1 - 负责人：XXX
            - [ ] 任务2 - 负责人：XXX
            ...

            ## 各参会者主要发言
            ### 参会者1
            - 主要观点...

            ### 参会者2
            - 主要观点...
            """,
            speakerNames,
            transcript
        );
    }
}
