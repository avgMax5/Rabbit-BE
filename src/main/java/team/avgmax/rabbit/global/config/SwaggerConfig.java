package team.avgmax.rabbit.global.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;
import io.swagger.v3.oas.models.security.SecurityScheme;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Rabbit API")
                        .description("Rabbit 백엔드 API 문서")
                        .version("1.0.0")
                )
                .servers(List.of(
                        new Server().url("https://rabbit.avgmax.team/api").description("운영 서버"),
                        new Server().url("https://avgmax.shinhanacademy.co.kr/api").description("학원 서버"),
                        new Server().url("http://localhost:8080/api").description("개발 서버")
                ))
                .components(new io.swagger.v3.oas.models.Components()
                        .addSecuritySchemes("bearerAuth", new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("UUID")
                                .description("Bearer 토큰을 입력하세요.")
                        )
                );
    }
}