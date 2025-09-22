package team.avgmax.rabbit.bunny.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import team.avgmax.rabbit.bunny.dto.response.FetchBunnyResponse;
import team.avgmax.rabbit.bunny.dto.response.MyBunnyResponse;

import java.util.List;

@Tag(name = "Bunny", description = "버니 API")
public interface BunnyApiDocs {

    // 버니 목록 조회
    @Operation(
        summary = "버니 목록 조회",
        description = "필터 값에 따라 버니 전체 목록을 조회합니다."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "목록 조회 성공",
            content = @Content(
                mediaType = "application/json",
                array = @ArraySchema(schema = @Schema(implementation = FetchBunnyResponse.class)),
                examples = @ExampleObject(
                    value = """
                    [
                        {
                            "bunny_id": "01HZXYBUNNY000000000000001",
                            "user_name": "사용자_01",
                            "bunny_name": "bunny-001",
                            "developer_type": "GROWTH",
                            "bunny_type": "A",
                            "position": "BACKEND",
                            "reliability": 87.55,
                            "current_price": 120000,
                            "closing_price": 110000,
                            "market_cap": 35000000,
                            "fluctuation_rate": 10.25,
                            "growth": 50,
                            "stability": 62,
                            "value": 31,
                            "popularity": 21,
                            "balance": 24,
                            "badges": [
                                "NAVER",
                                "SHINHAN"
                            ],
                            "like_count": "1234",
                            "created_at": "2025-09-15T14:30:00"
                        },
                        {
                            "bunny_id": "01HZXYBUNNY000000000000002",
                            "user_name": "사용자_02",
                            "bunny_name": "bunny-002",
                            "developer_type": "VALUE",
                            "bunny_type": "B",
                            "position": "FRONTEND",
                            "reliability": 73.20,
                            "current_price": 1300,
                            "closing_price": 1500,
                            "market_cap": 28000000,
                            "fluctuation_rate": -3.06,
                            "growth": 41,
                            "stability": 70,
                            "value": 65,
                            "popularity": 28,
                            "balance": 44,
                            "badges": [
                                "KAKAO"
                                "NAVER"
                            ],
                            "like_count": "786",
                            "created_at": "2025-09-16T09:45:00"
                        }
                    ]
                    """
                )
            )
        )
    })
    ResponseEntity<List<FetchBunnyResponse>> getBunnyList(
        @Parameter(description = "버니 목록 필터", example = "ALL, LATEST, CAPITALIZATION", required = true)
        String filter
    );

    // 버니 상세 조회
    @Operation(
        summary = "버니 상세 조회",
        description = "버니 이름으로 상세 정보를 조회합니다."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "상세 조회 성공",
            content = @Content(
                mediaType = "application/json",
                array = @ArraySchema(schema = @Schema(implementation = FetchBunnyResponse.class)),
                examples = @ExampleObject(
                    value = """
                    {
                        "bunny_id": "01HZXYBUNNY000000000000001",
                        "user_name": "사용자_01",
                        "bunny_name": "bunny-001",
                        "developer_type": "GROWTH",
                        "bunny_type": "A",
                        "position": "BACKEND",
                        "reliability": 87.55,
                        "current_price": 120000,
                        "closing_price": 110000,
                        "market_cap": 35000000,
                        "fluctuation_rate": 10.25,
                        "growth": 50,
                        "stability": 62,
                        "value": 31,
                        "popularity": 21,
                        "balance": 24,
                        "badges": [
                            "NAVER",
                            "SHINHAN"
                        ],
                        "like_count": "1234",
                        "created_at": "2025-09-15T14:30:00"
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
    ResponseEntity<FetchBunnyResponse> getBunny(
        @Parameter(description = "버니 이름", example = "bunny-001", required = true)
        String bunnyName
    );

    // 마이 버니 조회
    @Operation(
            summary = "마이 버니 조회",
            description = "현재 로그인한 사용자의 버니 정보를 조회합니다."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "마이 버니 조회 성공",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = MyBunnyResponse.class),
                examples = @ExampleObject(
                    value = """
                    {
                        "bunny_id": "01HZXYBUNNY000000000000001",
                        "user_id": "사용자_01"
                    }
                    """
                )
            )
        )
    })
    ResponseEntity<MyBunnyResponse> getMyBunny(
            @AuthenticationPrincipal Jwt jwt
    );

}
