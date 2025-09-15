package team.avgmax.rabbit.ai.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import team.avgmax.rabbit.ai.dto.response.ScoreResponse;
import team.avgmax.rabbit.user.dto.request.UpdatePersonalUserRequest;
import team.avgmax.rabbit.user.dto.response.*;

@Tag(name = "AI", description = "OpenAI chat API")
public interface ChatModelApiDocs {

    @Operation(
            summary = "질문",
            description = "원하는 질문의 응답을 바로 받아볼 수 있는 엔드포인트"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "gpt 질의 성공",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    value = "\"AI가 생성한 응답 텍스트 예시\""
                            )
                    )
            )
    })
    ResponseEntity<String> ask(
            @Parameter(description = "JWT 토큰", hidden = true) Jwt jwt,
            @Parameter(description = "사용자가 입력한 질문") @RequestParam String answer
    );

    @Operation(
        summary = "나의 점수 조회",
        description = "현재 로그인한 사용자의 AI 판단 결과를 조회합니다."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "나의 정보 조회 성공",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ScoreResponse.class),
                examples = @ExampleObject(
                    value = """
                    {
                        "growth":70,
                        "stability":80,
                        "value":75,
                        "popularity":65,
                        "balance":60
                    }
                    """
                )
            )
        )
    })
    ResponseEntity<ScoreResponse> score(
        @Parameter(description = "JWT 토큰", hidden = true) Jwt jwt
    );
}
