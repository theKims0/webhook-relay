package com.abdul.relay.config;

import com.abdul.relay.handler.RelayAgentHandler;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

/**
 * Konfigurasi WebSocket server.
 * Relay Agent (program lokal) akan connect ke endpoint /ws/agent
 * menggunakan query param: ?projectId={uuid}
 */
@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    private final RelayAgentHandler relayAgentHandler;

    public WebSocketConfig(RelayAgentHandler relayAgentHandler) {
        this.relayAgentHandler = relayAgentHandler;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(relayAgentHandler, "/ws/agent")
                .setAllowedOrigins("*"); // izinkan agent dari mana saja (lokal)
    }
}
