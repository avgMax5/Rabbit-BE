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
import org.springframework.web.bind.annotation.RequestParam;
import team.avgmax.rabbit.ai.dto.response.ScoreResponse;
import team.avgmax.rabbit.auth.oauth2.CustomOAuth2User;
import team.avgmax.rabbit.bunny.dto.response.AiBunnyResponse;

import org.springframework.security.oauth2.jwt.Jwt;

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
            @Parameter(description = "JWT 토큰", hidden = true) CustomOAuth2User customOAuth2User,
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
        @Parameter(description = "JWT 토큰", hidden = true) CustomOAuth2User customOAuth2User
    );

    @Operation(
        summary = "AI 응답 동기화",
        description = "버니의 AI 응답을 동기화합니다."
    )
    @ApiResponses(value = {
        @ApiResponse(
        responseCode = "200",
        description = "AI 응답 동기화 성공",
        content = @Content(
            mediaType = "application/json",
            schema = @Schema(implementation = AiBunnyResponse.class),
            examples = @ExampleObject(
                value = """
                {
                    "ai_review": "경영학 전공과 창업 경험을 토대로 사업 감각과 리더십을 겸비한 잠재력 높은 풀스택 전문가입니다.",
                    "ai_feedback": "## 기술 역량 및 학습 방향\n현재 기술스택이 없기 때문에, 우선 풀스택 개발에 필수적인 최신 프레임워크와 도구를 체계적으로 학습하는 것이 중요합니다. 온라인 강의, 오픈소스 프로젝트 참여, 그리고 개인 포트폴리오 개발을 통해 실무 경험을 쌓으세요. 특히, 클라이언트-서버 연동과 배포 경험을 강조하면 시장 내 경쟁력을 높일 수 있습니다. 이를 통해 신뢰도와 가치 지표를 향상시키며, 성장 가능성도 함께 증대시킬 수 있습니다.## 자격증과 경력 강화 전략 \n현재 보유 자격증은 기술적 신뢰성을 높이지만, 실무경력 부재는 시장 평가에 영향을 줄 수 있습니다. 관련 프로젝트 또는 인턴십 경험을 적극적으로 쌓아 경력을 채우고, 프로젝트 결과물을 공개하는 것도 효과적입니다. 또한, 정보보안 관련 자격증을 활용해 보안 역량을 강화하면 시장 내 안정성과 신뢰도를 상승시킬 수 있습니다. 이와 함께, 자격증 취득 계획을 지속적으로 세워 학습 의지와 성장성을 보여주는 것도 중요합니다.## 시장 내 인지도 및 거래 활성화 방안\n버니 지표의 모든 항목이 높게 평가된 만큼, 시장 내 인지도 향상과 거래 활성화를 위해 온라인 커뮤니티, SNS, 블로그 등을 활용해 자신의 활동을 적극 알리세요. 특히, 시장 트렌드에 맞는 콘텐츠를 제작하거나, 커뮤니티 참여를 통해 신뢰와 인지도를 높이면 인기와 균형 지표도 상승할 것입니다. 마지막으로, 각종 온라인 세미나, 워크숍 참여를 통해 네트워크를 확장하면서 시장 내 영향력을 확대하는 전략도 추천합니다."
                }
                """
                )
            )
        ),
        @ApiResponse(
            responseCode = "404",
            description = "해당 버니를 찾을 수 없습니다."
        )
    })
    ResponseEntity<AiBunnyResponse> sync(
        @Parameter(description = "JWT 토큰", hidden = true) Jwt jwt,
        @Parameter(description = "버니 이름") @RequestParam String bunnyName
    );
}
