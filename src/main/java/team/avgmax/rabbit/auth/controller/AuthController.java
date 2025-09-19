package team.avgmax.rabbit.auth.controller;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.time.Instant;
import java.util.Arrays;
import java.util.Map;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController implements AuthApiDocs {

    private final JwtDecoder jwtDecoder;
    private final JwtEncoder jwtEncoder;

    @Value("${app.security.jwt.access-expiry}")
    private long accessExpiry;

    @PostMapping("/refresh")
    public ResponseEntity<?> refreshToken(HttpServletRequest request, HttpServletResponse response) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return ResponseEntity.status(401).body(Map.of("error", "No refresh token"));
        }

        String refreshToken = Arrays.stream(cookies)
                .filter(c -> "REFRESH_TOKEN".equals(c.getName()))
                .findFirst()
                .map(Cookie::getValue)
                .orElse(null);

        if (refreshToken == null) {
            return ResponseEntity.status(401).body(Map.of("error", "No refresh token"));
        }

        try {
            Jwt jwt = jwtDecoder.decode(refreshToken);

            if (!"refresh".equals(jwt.getClaim("type"))) {
                return ResponseEntity.status(401).body(Map.of("error", "Invalid token type"));
            }

            Instant now = Instant.now();

            JwtClaimsSet accessClaims = JwtClaimsSet.builder()
                    .issuer("rabbit-app")
                    .issuedAt(now)
                    .expiresAt(now.plusSeconds(accessExpiry))
                    .subject(jwt.getSubject())
                    .claim("roles", jwt.getClaim("roles"))
                    .build();

            String newAccessToken = jwtEncoder.encode(JwtEncoderParameters.from(accessClaims)).getTokenValue();

            ResponseCookie accessCookie = ResponseCookie.from("ACCESS_TOKEN", newAccessToken)
                    .httpOnly(true)
                    .secure(true)
                    .sameSite("Strict")
                    .path("/")
                    .maxAge(accessExpiry)
                    .build();

            response.setHeader(HttpHeaders.SET_COOKIE, accessCookie.toString());
            
            return ResponseEntity.ok(Map.of("message", "new access token issued"));

        } catch (JwtException e) {
            return ResponseEntity.status(401).body(Map.of("error", "Invalid refresh token"));
        }
    }

    @GetMapping("/me")
    public Map<String, Object> user(@AuthenticationPrincipal Jwt jwt) {
        return Map.of(
                "sub", jwt.getSubject(),
                "roles", jwt.getClaim("roles")
        );
    }

    @GetMapping("/logout")
    public void logout(HttpServletResponse response) throws IOException {
        // ACCESS_TOKEN 만료
        ResponseCookie accessCookie = ResponseCookie.from("ACCESS_TOKEN", "")
                .httpOnly(true)
                .secure(true)
                .sameSite("Strict")
                .path("/")
                .maxAge(0)  // 즉시 만료
                .build();

        // REFRESH_TOKEN 만료
        ResponseCookie refreshCookie = ResponseCookie.from("REFRESH_TOKEN", "")
                .httpOnly(true)
                .secure(true)
                .sameSite("Strict")
                .path("/auth/refresh")
                .maxAge(0)  // 즉시 만료
                .build();

        response.addHeader(HttpHeaders.SET_COOKIE, accessCookie.toString());
        response.addHeader(HttpHeaders.SET_COOKIE, refreshCookie.toString());

        response.sendRedirect("/");
    }

    @GetMapping("/dummy")
    public ResponseEntity<?> dummy(HttpServletResponse response, @RequestParam("userId") String personalUserId) {
        Instant now = Instant.now();

        JwtClaimsSet accessClaims = JwtClaimsSet.builder()
                .issuer("rabbit-app")
                .issuedAt(now)
                .expiresAt(now.plusSeconds(accessExpiry))
                .subject(personalUserId) // 더미 유저 아이디
                .claim("roles", new String[]{"ROLE_USER"})
                .build();

        JwsHeader jwsHeader = JwsHeader.with(MacAlgorithm.HS256).build();
        String accessToken = jwtEncoder.encode(JwtEncoderParameters.from(jwsHeader, accessClaims)).getTokenValue();

        ResponseCookie accessCookie = ResponseCookie.from("ACCESS_TOKEN", accessToken)
                .httpOnly(true)
                .secure(true)
                .sameSite("Strict")
                .path("/")
                .maxAge(accessExpiry)
                .build();

        response.setHeader(HttpHeaders.SET_COOKIE, accessCookie.toString());

        return ResponseEntity.ok(Map.of("message", "더미 액세스 토큰 발급: " + accessToken));
    }
}