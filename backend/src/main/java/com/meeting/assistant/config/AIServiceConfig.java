package com.meeting.assistant.config;

import com.meeting.assistant.ai.AIService;
import com.meeting.assistant.ai.LocalWhisperProvider;
import com.meeting.assistant.ai.OpenAIProvider;
import com.meeting.assistant.ai.ParaformerProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * AI 服务配置类
 * 根据配置选择使用 OpenAI API、本地 Whisper 或 Paraformer
 */
@Slf4j
@Configuration
public class AIServiceConfig {

    @Value("${ai.provider:openai}")
    private String aiProvider;

    /**
     * 根据配置选择 AI 服务提供者
     */
    @Bean
    @Primary
    public AIService aiService(OpenAIProvider openAIProvider,
                               LocalWhisperProvider localWhisperProvider,
                               ParaformerProvider paraformerProvider) {
        log.info("AI Provider configuration: {}", aiProvider);

        if ("local-whisper".equalsIgnoreCase(aiProvider)) {
            log.info("Using Local Whisper for audio transcription");
            return localWhisperProvider;
        } else if ("paraformer".equalsIgnoreCase(aiProvider)) {
            log.info("Using Paraformer for streaming audio transcription");
            return paraformerProvider;
        } else {
            log.info("Using OpenAI Whisper API for audio transcription");
            return openAIProvider;
        }
    }
}
