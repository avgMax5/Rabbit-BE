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
import team.avgmax.rabbit.bunny.entity.enums.ChartInterval;
import team.avgmax.rabbit.bunny.dto.orderBook.OrderBookSnapshot;
import team.avgmax.rabbit.bunny.dto.request.OrderRequest;
import team.avgmax.rabbit.bunny.dto.response.*;
import team.avgmax.rabbit.bunny.dto.response.FetchBunnyResponse;
import team.avgmax.rabbit.bunny.dto.response.MyBunnyResponse;
import team.avgmax.rabbit.bunny.dto.response.RabbitIndexResponse;

import java.util.List;

@Tag(name = "Bunny", description = "버니 API")
public interface BunnyApiDocs {
    // ---------------- RABBIT 지수 조회 ----------------
    @Operation(
        summary = "RABBIT 지수 조회",
        description = "RABBIT 지수를 조회합니다."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "RABBIT 지수 조회 성공",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = RabbitIndexResponse.class),
                examples = @ExampleObject(
                    value = """
                    {
                        "rabbit_index": 105.25
                    }
                    """
                )
            )
        )
    })
    ResponseEntity<RabbitIndexResponse> getRabbitIndex();

    // ---------------- 버니 목록 조회 ----------------
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
                        "badges": ["NAVER", "SHINHAN"],
                        "like_count": "1234",
                        "created_at": "2025-09-15T14:30:00"
                      }
                    ]
                    """
                )
            )
        )
    })
    ResponseEntity<List<FetchBunnyResponse>> getBunnyList(
        @Parameter(description = "버니 목록 필터", example = "ALL, LATEST, CAPITALIZATION")
        String filter
    );

    // ---------------- 버니 상세 조회 ----------------
    @Operation(summary = "버니 상세 조회", description = "버니 이름으로 상세 정보를 조회합니다.")
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "상세 조회 성공",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = FetchBunnyResponse.class),
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
                      "badges": ["NAVER", "SHINHAN"],
                      "like_count": "1234",
                      "created_at": "2025-09-15T14:30:00"
                    }
                    """
                )
            )
        ),
        @ApiResponse(responseCode = "404", description = "해당 버니를 찾을 수 없습니다.")
    })
    ResponseEntity<FetchBunnyResponse> getBunny(
        @Parameter(description = "버니 이름", example = "bunny-001", required = true)
        String bunnyName
    );

    // ---------------- 마이 버니 조회 ----------------
    @Operation(summary = "마이 버니 조회", description = "현재 로그인한 사용자의 버니 정보를 조회합니다.")
    @ApiResponse(
        responseCode = "200",
        description = "마이 버니 조회 성공",
        content = @Content(
            mediaType = "application/json",
            schema = @Schema(implementation = MyBunnyResponse.class),
            examples = @ExampleObject(
                value = """
                {
                    "bunny_id": "01BUNNY0010000000000000033",
                    "user_name": "사용자33",
                    "user_image": "https://picsum.photos/200",
                    "user_carrot": 100000000,
                    "bunny_name": "bunny-033",
                    "bunny_type": "C",
                    "developer_type": "BALANCE",
                    "position": "BACKEND",
                    "badges": [
                        "KAKAO",
                        "NAVER",
                        "SHINHAN"
                    ],
                    "today_time": "2025-09-23",
                    "monthly_growth_rates": [],
                    "price_history": [],
                    "reliability": 99,
                    "current_price": 71,
                    "closing_price": 158,
                    "market_cap": 71000000,
                    "avg_bunny_type_vs_me": -33.289,
                    "avg_position_vs_me": -72.192,
                    "avg_dev_type_vs_me": -86.5027,
                    "competitors": [
                        {
                        "bunnyId": "01BUNNY0010000000000000035",
                        "bunnyName": "bunny-035",
                        "userImage": "https://picsum.photos/200",
                        "rank": 44,
                        "marketCap": 73000000,
                        "growthRate": 19.67
                        },
                        {
                        "bunnyId": "01BUNNY0010000000000000031",
                        "bunnyName": "bunny-031",
                        "userImage": "https://picsum.photos/200",
                        "rank": 46,
                        "marketCap": 69400000,
                        "growthRate": -76.22
                        }
                    ],
                    "my_growth_rate": -55.06,
                    "growth": 88,
                    "stability": 57,
                    "value": 63,
                    "popularity": 5,
                    "balance": 86,
                    "holder_types": [
                        {
                        "developerType": "BASIC",
                        "percentage": 28.69,
                        "count": 5
                        },
                        {
                        "developerType": "BALANCE",
                        "percentage": 4.38,
                        "count": 1
                        },
                        {
                        "developerType": "GROWTH",
                        "percentage": 12.45,
                        "count": 2
                        },
                        {
                        "developerType": "STABLE",
                        "percentage": 1.04,
                        "count": 2
                        }
                    ],
                    "holders": [
                        {
                            "userId": "01RABBIT010000000000000030",
                            "userName": "사용자30",
                            "userImg": "https://picsum.photos/200",
                            "holdQuantity": 86382
                        },
                        {
                            "userId": "01RABBIT010000000000000085",
                            "userName": "사용자85",
                            "userImg": "https://picsum.photos/200",
                            "holdQuantity": 84847
                        },
                        {
                            "userId": "01RABBIT010000000000000003",
                            "userName": "사용자3",
                            "userImg": "https://picsum.photos/200",
                            "holdQuantity": 80826
                        },
                        {
                            "userId": "01RABBIT010000000000000099",
                            "userName": "사용자_099",
                            "userImg": "https://picsum.photos/200",
                            "holdQuantity": 49662
                        },
                        {
                            "userId": "01RABBIT010000000000000052",
                            "userName": "사용자52",
                            "userImg": "https://picsum.photos/200",
                            "holdQuantity": 43776
                        },
                        {
                            "userId": "01RABBIT010000000000000015",
                            "userName": "사용자15",
                            "userImg": "https://picsum.photos/200",
                            "holdQuantity": 42974
                        },
                        {
                            "userId": "01RABBIT010000000000000036",
                            "userName": "사용자36",
                            "userImg": "https://picsum.photos/200",
                            "holdQuantity": 38119
                        },
                        {
                            "userId": "01RABBIT010000000000000029",
                            "userName": "사용자29",
                            "userImg": "https://picsum.photos/200",
                            "holdQuantity": 28614
                        },
                        {
                            "userId": "01RABBIT010000000000000042",
                            "userName": "사용자42",
                            "userImg": "https://picsum.photos/200",
                            "holdQuantity": 6702
                        }
                    ]
                }
                """
            )
        )
    )
    ResponseEntity<MyBunnyResponse> getMyBunny(@AuthenticationPrincipal Jwt jwt);

    // ---------------- 거래 차트 조회 ----------------
    @Operation(summary = "거래 차트 조회", description = "특정 버니의 거래 차트를 조회합니다.")
    ResponseEntity<ChartResponse> getChart(
        @Parameter(description = "버니 이름", example = "bunny-001") String bunnyName,
        @Parameter(description = "차트 구간", example = "DAILY") ChartInterval interval
    );

    // ---------------- 특정 버니 마이 리스트 조회 ----------------
    @Operation(summary = "특정 버니 마이 리스트 조회", description = "특정 버니에 대해 사용자가 가진 주문 내역을 조회합니다.")
    ResponseEntity<OrderListResponse> getMyBunnyList(
        @AuthenticationPrincipal Jwt jwt,
        @Parameter(description = "버니 이름", example = "bunny-001") String bunnyName
    );

    // ---------------- 버니 좋아요 추가 ----------------
    @Operation(summary = "버니 좋아요 추가", description = "해당 버니에 좋아요를 추가합니다.")
    ResponseEntity<Void> addBunnyLike(@AuthenticationPrincipal Jwt jwt,
                                      @Parameter(description = "버니 이름", example = "bunny-001") String bunnyName);

    // ---------------- 버니 좋아요 취소 ----------------
    @Operation(summary = "버니 좋아요 취소", description = "해당 버니에 추가한 좋아요를 취소합니다.")
    ResponseEntity<Void> cancelBunnyLike(@AuthenticationPrincipal Jwt jwt,
                                         @Parameter(description = "버니 이름", example = "bunny-001") String bunnyName);

    // ---------------- 거래 주문 요청 ----------------
    @Operation(summary = "거래 주문 요청", description = "특정 버니에 대해 매수/매도 주문을 생성합니다.")
    ResponseEntity<OrderResponse> createOrder(
        @Parameter(description = "버니 이름", example = "bunny-001") String bunnyName,
        @Parameter(description = "주문 요청 바디") OrderRequest request,
        @AuthenticationPrincipal Jwt jwt
    );

    // ---------------- 거래 주문 취소 ----------------
    @Operation(summary = "거래 주문 취소", description = "주문 ID를 이용해 주문을 취소합니다.")
    ResponseEntity<Void> cancelOrder(
        @Parameter(description = "버니 이름", example = "bunny-001") String bunnyName,
        @Parameter(description = "주문 ID", example = "order-123") String orderId,
        @AuthenticationPrincipal Jwt jwt
    );

    // ---------------- 호가창 스냅샷 조회 ----------------
    @Operation(summary = "호가창 스냅샷 조회", description = "특정 버니의 현재 호가창 정보를 조회합니다.")
    ResponseEntity<OrderBookSnapshot> getOrderBookSnapshot(
        @Parameter(description = "버니 이름", example = "bunny-001") String bunnyName
    );
}
