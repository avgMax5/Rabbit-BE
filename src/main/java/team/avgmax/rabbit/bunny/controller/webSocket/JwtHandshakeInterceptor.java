package team.avgmax.rabbit.bunny.controller.webSocket;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.Cookie;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.core.convert.converter.Converter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.server.resource.web.BearerTokenResolver;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.util.Arrays;
import java.util.Map;
import java.util.Objects;
import java.util.Set;


@Slf4j
@Component
@RequiredArgsConstructor
public class JwtHandshakeInterceptor implements HandshakeInterceptor {

    private static final String AUTH_USER = "user";
    private static final Set<String> TOKEN_COOKIE_NAMES = Set.of("access_token", "jwt_token");
    private static final int TOKEN_PREVIEW = 20;

    private final BearerTokenResolver cookieBearerTokenResolver;
    private final JwtDecoder jwtDecoder;
    private final Converter<Jwt, ? extends AbstractAuthenticationToken> jwtAuthenticationConverter;

    @Override
    public boolean beforeHandshake(
            @NonNull ServerHttpRequest request,
            @NonNull ServerHttpResponse response,
            @NonNull WebSocketHandler wsHandler,
            @NonNull Map<String, Object> attributes
    ) {
        if (!(request instanceof ServletServerHttpRequest servletRequest)) {
            return false;
        }

        final HttpServletRequest httpRequest = servletRequest.getServletRequest();

        try {
            // 1) 토큰 해석 (Header → Resolver(Cookie) → 직접 Cookie 탐색)
            final String token = resolveToken(httpRequest);

            if (token != null) {
                final Jwt jwt = jwtDecoder.decode(token);
                final Authentication auth = jwtAuthenticationConverter.convert(jwt);

                if (auth == null) {
                    // 토큰이 있으나 변환 실패 시 예외로 처리 → 아래 catch 에서 false 리턴
                    throw new IllegalStateException("JWT converted to null Authentication");
                }

                attributes.put(AUTH_USER, auth);
                if (log.isDebugEnabled()) {
                    log.debug("WebSocket Authentication successful for user: {}", safeName(auth.getName()));
                }
                return true;
            }

            // token == null 인 경우: 기존 로직대로 경고 + 테스트 허용 로그 후 연결 허용
            log.warn("WebSocket Authentication failed: No token found");
            log.info("WebSocket: Allowing connection without authentication for testing");
            return true;

        } catch (Exception e) {
            // 스택트레이스 포함해 에러 로깅
            log.error("WebSocket Authentication failed with exception", e);
            return false;
        }
    }

    @Override
    public void afterHandshake(
            @NonNull ServerHttpRequest request,
            @NonNull ServerHttpResponse response,
            @NonNull WebSocketHandler wsHandler,
            Exception e
    ) {}

    private String resolveToken(HttpServletRequest req) {
        // 1. Bearer Header 방식
        final String authHeader = req.getHeader("Authorization");
        if (authHeader != null) {
            if (log.isDebugEnabled()) {
                log.debug("WebSocket Auth Header: {}", mask(authHeader, TOKEN_PREVIEW));
            }
            if (authHeader.startsWith("Bearer ")) {
                final String token = authHeader.substring(7);
                if (log.isDebugEnabled()) {
                    log.debug("WebSocket Token from header: {}", preview(token, TOKEN_PREVIEW));
                }
                return token;
            }
        }

        // 2. BearerTokenResolver (Cookie 방식)
        final String resolved = cookieBearerTokenResolver.resolve(req);
        if (log.isDebugEnabled()) {
            log.debug("WebSocket Token from cookie resolver: {}",
                    resolved != null ? preview(resolved, TOKEN_PREVIEW) : "null");
        }
        if (resolved != null) return resolved;

        // 3. Cookie 에서 직접 Token 찾기
        final Cookie[] cookies = req.getCookies();
        if (cookies != null && cookies.length > 0) {
            // 디버깅용: 쿠키 목록 미리보기 (값은 마스킹)
            if (log.isDebugEnabled()) {
                Arrays.stream(cookies).forEach(c ->
                        log.debug("Cookie: {} = {}", c.getName(), preview(c.getValue(), TOKEN_PREVIEW))
                );
            }

            return Arrays.stream(cookies)
                    .filter(Objects::nonNull)
                    .filter(c -> TOKEN_COOKIE_NAMES.contains(c.getName()))
                    .findFirst()
                    .map(c -> {
                        if (log.isDebugEnabled()) {
                            log.debug("Found token in cookie: {}", c.getName());
                        }
                        return c.getValue();
                    })
                    .orElse(null);
        }
        return null;
    }

    private static String preview(String value, int n) {
        if (value == null) return "null";
        final int end = Math.min(n, value.length());
        return value.substring(0, end) + (value.length() > end ? "..." : "");
    }

    private static String mask(String value, int n) { return preview(value, n); }

    private static String safeName(String name) {
        return (name == null || name.isBlank()) ? "<anonymous>" : name;
    }
}
