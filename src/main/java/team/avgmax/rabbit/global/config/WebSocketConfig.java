package team.avgmax.rabbit.global.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

// Spring-WebSocket dependency
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {

        // [서버 → 클라이언트] 브로커가 라우팅할 목적지(prefix)
        // - /topic : 방송형(Pub/Sub). 다수 구독자가 함께 받는 채널 (예: 호가창 스냅샷)
        // - /queue : 1:1 지향(개인용). convertAndSendToUser(...)와 궁합 (예: 주문 결과/알림)
        registry.enableSimpleBroker("/topic");

        // [클라이언트 → 서버] @MessageMapping 메서드로 들어오는 경로(prefix)
        registry.setApplicationDestinationPrefixes("/app");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {

        // Native WebSocket
        // 네이티브 사용 → new WebSocket("/ws")
        registry.addEndpoint("/ws/orderBook")
                // 개발 시 - 와일드카드("*"), 운영 시 - 화이트 리스트("https://app.example.com", "https://admin.example.com")
                .setAllowedOriginPatterns("*"); // CORS 허용

        // SockJS fallback (네이티브가 되지 않을 때 안전망)
        // SockJS 사용 → new SockJS("/ws")
        registry.addEndpoint("/ws/orderBook")
                .setAllowedOriginPatterns("*")
                .withSockJS();
    }
}
