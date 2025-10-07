package com.meeting.assistant.config;

import com.meeting.assistant.websocket.AudioStreamHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;
import org.springframework.web.socket.server.standard.ServletServerContainerFactoryBean;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    private final AudioStreamHandler audioStreamHandler;

    public WebSocketConfig(AudioStreamHandler audioStreamHandler) {
        this.audioStreamHandler = audioStreamHandler;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(audioStreamHandler, "/ws/audio-stream")
            .setAllowedOrigins("*");  // 生产环境需要配置具体的origins
    }

    /**
     * 配置WebSocket消息缓冲区大小
     * 音频数据可能较大，需要增加缓冲区限制
     */
    @Bean
    public ServletServerContainerFactoryBean createWebSocketContainer() {
        ServletServerContainerFactoryBean container = new ServletServerContainerFactoryBean();
        // 设置文本消息缓冲区大小为 1MB
        container.setMaxTextMessageBufferSize(1024 * 1024);
        // 设置二进制消息缓冲区大小为 1MB
        container.setMaxBinaryMessageBufferSize(1024 * 1024);
        // 设置会话空闲超时为 10 分钟
        container.setMaxSessionIdleTimeout(600000L);
        return container;
    }
}
