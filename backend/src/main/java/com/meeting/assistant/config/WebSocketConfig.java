package com.meeting.assistant.config;

import com.meeting.assistant.websocket.AudioStreamHandler;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

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
}
