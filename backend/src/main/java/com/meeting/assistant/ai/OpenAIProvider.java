package com.meeting.assistant.ai;

import com.meeting.assistant.entity.Speaker;
import com.meeting.assistant.util.AudioUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * OpenAI GPT-4o实现
 * 支持音频输入和文本总结
 */
@Slf4j
@Service
public class OpenAIProvider implements AIService {

    private final ChatClient chatClient;
    private final RestTemplate restTemplate;

    @Value("${spring.ai.openai.api-key}")
    private String apiKey;

    @Value("${spring.ai.openai.base-url}")
    private String baseUrl;

    public OpenAIProvider(ChatClient.Builder chatClientBuilder) {
        this.chatClient = chatClientBuilder.build();
        this.restTemplate = new RestTemplate();
    }

    @Override
    public String transcribe(byte[] audioData) {
        log.info("Transcribing audio with Whisper API, PCM size: {} bytes", audioData.length);
        try {
            // 将 PCM 数据转换为 WAV 格式
            // Android 端: 16kHz, Mono, 16-bit PCM
            byte[] wavData = AudioUtils.pcmToWav(audioData, 16000, 1, 16);
            log.info("Converted PCM to WAV, WAV size: {} bytes", wavData.length);

            // 构建请求头
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);
            headers.setBearerAuth(apiKey);

            // 构建请求体
            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            body.add("file", new org.springframework.core.io.ByteArrayResource(wavData) {
                @Override
                public String getFilename() {
                    return "audio.wav";
                }
            });
            body.add("model", "whisper-1");
            body.add("language", "zh"); // 中文
            body.add("prompt", "请使用简体中文转录"); // 引导输出简体中文

            HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

            // 调用 Whisper API
            String whisperUrl = baseUrl + "/v1/audio/transcriptions";
            ResponseEntity<Map> response = restTemplate.exchange(
                whisperUrl,
                HttpMethod.POST,
                requestEntity,
                Map.class
            );

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                String text = (String) response.getBody().get("text");
                log.info("Transcription completed, text length: {}", text.length());
                return text;
            } else {
                throw new RuntimeException("Whisper API returned unexpected response");
            }
        } catch (Exception e) {
            log.error("Transcription failed", e);
            throw new RuntimeException("音频转录失败: " + e.getMessage(), e);
        }
    }

    @Override
    public String summarize(String transcript, List<Speaker> speakers) {
        log.info("Generating meeting summary with GPT-4o");

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
        return "GPT-4o (OpenAI)";
    }

    private String buildSummaryPrompt(String transcript, String speakerNames) {
        return String.format("""
            请根据以下会议转录内容生成结构化总结：

            参会人员：%s

            转录内容：
            %s

            请按以下格式输出（使用Markdown格式）：

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
