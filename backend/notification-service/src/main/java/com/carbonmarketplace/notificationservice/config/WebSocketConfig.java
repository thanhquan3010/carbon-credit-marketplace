package com.carbonmarketplace.notificationservice.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {
    
    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        // Enable a simple in-memory broker
        registry.enableSimpleBroker("/topic", "/queue");
        // Set application destination prefix for messages bound for @MessageMapping methods
        registry.setApplicationDestinationPrefixes("/app");
        // Set user destination prefix for user-specific messages
        registry.setUserDestinationPrefix("/user");
    }
    
    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // Register WebSocket endpoint for clients to connect
        registry.addEndpoint("/ws/notifications")
                .setAllowedOrigins("*") // Configure based on your CORS requirements
                .withSockJS(); // Enable SockJS fallback
        
        // Register endpoint without SockJS for native WebSocket clients
        registry.addEndpoint("/ws/notifications")
                .setAllowedOrigins("*");
    }
}
