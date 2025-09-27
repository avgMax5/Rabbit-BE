package team.avgmax.rabbit.global.config;

import lombok.RequiredArgsConstructor;

import org.springframework.core.convert.converter.Converter;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

import team.avgmax.rabbit.auth.oauth2.CookieBearerTokenResolver;
import team.avgmax.rabbit.bunny.controller.orderBook.JwtHandshakeInterceptor;

// Spring-WebSocket dependency
@Configuration
@RequiredArgsConstructor
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final CookieBearerTokenResolver cookieBearerTokenResolver;
    private final JwtDecoder jwtDecoder;
    private final Converter<Jwt, ? extends AbstractAuthenticationToken> jwtAuthenticationConverter;

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
        registry.addEndpoint("/ws/orderBook")
                .addInterceptors(new JwtHandshakeInterceptor(
                        cookieBearerTokenResolver,
                        jwtDecoder,
                        jwtAuthenticationConverter
                ))
                // 개발 시 - 와일드카드("*"), 운영 시 - 화이트 리스트("https://app.example.com", "https://admin.example.com")
                .setAllowedOriginPatterns("*")
                .withSockJS();
    }
}
