package team.avgmax.rabbit.auth.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.jwt.Jwt;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.util.Map;

@Tag(name = "Auth", description = "인증 API")
public interface AuthApiDocs {

    @Operation(
        summary = "엑세스 토큰 발급",
        description = "액세스 토큰이 만료되어 새로운 엑세스 토큰을 발급받습니다."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "엑세스 토큰 발급 성공",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(
                    value = """
                    {
                        "message": "new access token issued"
                    }
                    """
                )
            )
        ),
        @ApiResponse(
            responseCode = "401",
            description = "인증 실패",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(
                    value = """
                    {
                        "error": "No refresh token"
                    }
                    """
                )
            )
        )
    })
    ResponseEntity<?> refreshToken(HttpServletRequest request, HttpServletResponse response);

    @Operation(
        summary = "현재 사용자 정보 조회",
        description = "현재 로그인한 사용자의 정보를 조회합니다."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "사용자 정보 조회 성공",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(
                    value = """
                    {
                        "sub": "01HZXUSER00000000000000001",
                        "roles": ["ROLE_USER"]
                    }
                    """
                )
            )
        )
    })
    Map<String, Object> user(Jwt jwt);

    @Operation(
        summary = "로그아웃",
        description = "사용자를 로그아웃하고 모든 토큰을 만료시킵니다."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "204",
            description = "로그아웃 성공"
        )
    })
    ResponseEntity<Void> logout(HttpServletResponse response);

    @Operation(
        summary = "더미 토큰 발급",
        description = "개발 및 테스트를 위한 더미 액세스 토큰을 발급합니다."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "더미 토큰 발급 성공",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(
                    value = """
                    {
                        "message": "더미 액세스 토큰 발급: {accessToken}"
                    }
                    """
                )
            )
        )
    })
    ResponseEntity<?> dummy(
        HttpServletResponse response,
        @Parameter(description = "더미 사용자 ID", required = true) String personalUserId
    );
}
