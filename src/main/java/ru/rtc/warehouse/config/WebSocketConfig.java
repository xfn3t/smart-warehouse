package ru.rtc.warehouse.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.lang.NonNull;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.*;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    /**
     * Регистрируем STOMP endpoints — точки подключения клиентов.
     * Можно подключаться как через "/ws", так и через "/api/ws/dashboard".
     */
    @Override
    public void registerStompEndpoints(@NonNull StompEndpointRegistry registry) {
        registry.addEndpoint("/ws") // фронт-энд
                .setAllowedOriginPatterns("*")
                .withSockJS();

        registry.addEndpoint("/api/ws/dashboard") // внутренний дашборд/роботы
                .setAllowedOriginPatterns("*")
                .withSockJS();
    }

    /**
     * Конфигурация простого брокера сообщений
     */
    @Override
    public void configureMessageBroker(@NonNull MessageBrokerRegistry registry) {
        // Клиент шлёт на /app/...
        registry.setApplicationDestinationPrefixes("/app");

        // Клиент слушает /topic/...
        registry.enableSimpleBroker("/topic");
    }
}
