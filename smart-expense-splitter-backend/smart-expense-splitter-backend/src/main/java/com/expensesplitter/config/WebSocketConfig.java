package com.expensesplitter.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * Wires up STOMP-over-WebSocket messaging.
 *
 * Two things are being configured here:
 * 1. The endpoint the browser connects to (/ws) to open the socket, with SockJS
 *    as a fallback for browsers/networks that can't do a raw WebSocket handshake.
 * 2. The message broker: a "simple broker" is an in-memory pub/sub broker built
 *    into Spring — good enough for a single-instance app. In a real multi-instance
 *    production deployment you'd swap this for an external broker (RabbitMQ with
 *    STOMP plugin) so messages fan out across all app instances, not just one.
 *
 * Flow: server calls SimpMessagingTemplate.convertAndSend("/topic/group/5", payload)
 * -> broker pushes it to every client currently subscribed to /topic/group/5.
 * We never receive messages FROM the client in this app (no @MessageMapping) —
 * it's server-to-client push only, so /app is configured but unused for now.
 */
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    // Same property used by SecurityConfig's CORS setup — one source of truth
    // for "which frontend URLs are allowed to talk to this backend."
    @Value("${app.cors.allowed-origins:http://localhost:5173}")
    private String allowedOrigins;

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns(allowedOrigins.split(","))
                .withSockJS(); // fallback transport (long-polling etc.) if native WS is blocked
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.enableSimpleBroker("/topic");
        registry.setApplicationDestinationPrefixes("/app");
    }
}
