package team.avgmax.rabbit.global.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
public class CorsConfig {
    @Value("${cors.allowed-origins}")
    private List<String> allowedOrigins;

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        configuration.setAllowedOrigins(allowedOrigins);
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS", "HEAD"));
        configuration.setAllowedHeaders(List.of("*")); 
        configuration.setAllowCredentials(true); // 쿠키/Authorization 헤더 허용 (JWT 쿠키 전송하려면 필수)

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource(); // 허용할 경로 패턴 등록
        source.registerCorsConfiguration("/**", configuration);

        return source;
    }
}