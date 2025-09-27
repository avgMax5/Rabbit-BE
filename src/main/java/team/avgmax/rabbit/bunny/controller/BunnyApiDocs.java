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

import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.List;

import team.avgmax.rabbit.bunny.entity.enums.ChartInterval;
import team.avgmax.rabbit.bunny.dto.orderBook.OrderBookSnapshot;
import team.avgmax.rabbit.bunny.dto.request.OrderRequest;
import team.avgmax.rabbit.bunny.dto.response.*;


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

    // ---------------- 업데이트 알림 목록 조회 ----------------
    @Operation(
        summary = "업데이트 알림 목록 조회",
        description = "업데이트 알림 목록을 조회합니다."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "업데이트 알림 목록 조회 성공",
            content = @Content(
                mediaType = "application/json",
                array = @ArraySchema(schema = @Schema(implementation = String.class)),
                examples = @ExampleObject(
                    value = """
                    [
                        "bunny-001",
                        "bunny-002",
                        "bunny-003"
                    ]
                    """
                )
            )
        )
    })
    ResponseEntity<List<String>> getUpdateAlerts(); 

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
                    {
                        "content": [
                            {
                            "bunny_id": "01BUNNY0010000000000000030",
                            "user_name": "사용자30",
                            "bunny_name": "bunny-030",
                            "developer_type": "GROWTH",
                            "bunny_type": "A",
                            "position": "FULLSTACK",
                            "reliability": 6,
                            "current_price": 100727,
                            "closing_price": 121584,
                            "market_cap": 100727000,
                            "fluctuation_rate": null,
                            "growth": 66,
                            "stability": 70,
                            "value": 48,
                            "popularity": 51,
                            "balance": 86,
                            "badges": [
                                "KAKAO",
                                "NAVER",
                                "SHINHAN"
                            ],
                            "ai_review": "최신 트렌드와 프레임워크에 강해 시장의 주목을 받는 인기주 개발자",
                            "like_count": 14,
                            "spec": {
                                "name": "사용자30",
                                "birthdate": "1999-05-25",
                                "email": "user030@google.com",
                                "phone": "010-0000-0030",
                                "image": "https://picsum.photos/200",
                                "resume": "https://example.com/resume.pdf",
                                "position": "FULLSTACK",
                                "link": [
                                {
                                    "sns_id": "01RABBIT070000000000003001",
                                    "url": "https://www.linkedin.com/user30",
                                    "type": "LINKEDIN",
                                    "favicon": "https://linkedin.com/favicon.ico"
                                }
                                ],
                                "skill": [
                                "Swift",
                                "Node.js",
                                "Javascript",
                                "React",
                                "PHP"
                                ],
                                "certification": [
                                {
                                    "certification_id": "01RABBIT040000000000003001",
                                    "certificate_url": "https://example.com/certificate.pdf",
                                    "name": "리눅스마스터",
                                    "ca": "한국산업인력공단",
                                    "cdate": "2023-07-03"
                                },
                                {
                                    "certification_id": "01RABBIT040000000000003002",
                                    "certificate_url": "https://example.com/certificate.pdf",
                                    "name": "ADSP",
                                    "ca": "한국산업인력공단",
                                    "cdate": "2025-05-03"
                                }
                                ],
                                "career": [
                                {
                                    "career_id": "01RABBIT060000000000003001",
                                    "company_name": "AVGMAX",
                                    "status": "EMPLOYED",
                                    "position": "플랫폼 백엔드 엔지니어",
                                    "start_date": "2023-03-01",
                                    "end_date": "2023-08-15",
                                    "certificate_url": "https://example.com/certificate.pdf"
                                },
                                {
                                    "career_id": "01RABBIT060000000000003002",
                                    "company_name": "구글",
                                    "status": "UNEMPLOYED",
                                    "position": "서버팀 테크리더",
                                    "start_date": "2023-03-01",
                                    "end_date": "2020-08-15",
                                    "certificate_url": "https://example.com/certificate.pdf"
                                },
                                {
                                    "career_id": "01RABBIT060000000000003003",
                                    "company_name": "당근",
                                    "status": "UNEMPLOYED",
                                    "position": "서버팀 테크리더",
                                    "start_date": "2023-03-01",
                                    "end_date": "2025-08-15",
                                    "certificate_url": "https://example.com/certificate.pdf"
                                }
                                ],
                                "education": [
                                {
                                    "education_id": "01RABBIT050000000000003001",
                                    "school_name": "신한고등학교",
                                    "status": "GRADUATED",
                                    "major": "인문계",
                                    "start_date": "2013-03-01",
                                    "end_date": "2013-02-15",
                                    "certificate_url": "https://example.com/certificate.pdf"
                                },
                                {
                                    "education_id": "01RABBIT050000000000003002",
                                    "school_name": "서울시립대학교",
                                    "status": "ENROLLED",
                                    "major": "물리학과",
                                    "start_date": "2010-03-01",
                                    "end_date": "2019-02-15",
                                    "certificate_url": "https://example.com/certificate.pdf"
                                }
                                ],
                                "ai_review": "산업공학 전공과 글로벌 프로젝트 리더 경험을 바탕으로 전략적 사고와 팀 조율 능력을 갖춘 잠재력 높은 프론트엔드 전문가입니다."
                            },
                            "created_at": "2025-09-22T10:00:00"
                            },
                            {
                            "bunny_id": "01BUNNY0010000000000000031",
                            "user_name": "사용자31",
                            "bunny_name": "bunny-031",
                            "developer_type": "POPULAR",
                            "bunny_type": "B",
                            "position": "BACKEND",
                            "reliability": 67,
                            "current_price": 694,
                            "closing_price": 2918,
                            "market_cap": 69400000,
                            "fluctuation_rate": null,
                            "growth": 79,
                            "stability": 50,
                            "value": 58,
                            "popularity": 54,
                            "balance": 67,
                            "badges": [],
                            "ai_review": "최신 트렌드와 프레임워크에 강해 시장의 주목을 받는 인기주 개발자",
                            "like_count": 7,
                            "spec": {
                                "name": "사용자31",
                                "birthdate": "1994-10-24",
                                "email": "user031@google.com",
                                "phone": "010-0000-0031",
                                "image": "https://picsum.photos/200",
                                "resume": "https://example.com/resume.pdf",
                                "position": "BACKEND",
                                "link": [
                                {
                                    "sns_id": "01RABBIT070000000000003101",
                                    "url": "https://www.linkedin.com/user31",
                                    "type": "LINKEDIN",
                                    "favicon": "https://linkedin.com/favicon.ico"
                                },
                                {
                                    "sns_id": "01RABBIT070000000000003102",
                                    "url": "https://www.tistory.com/user31",
                                    "type": "TISTORY",
                                    "favicon": "https://tistory.com/favicon.ico"
                                }
                                ],
                                "skill": [
                                "SpringBoot",
                                "Node.js",
                                "Kubernetes"
                                ],
                                "certification": [
                                {
                                    "certification_id": "01RABBIT040000000000003101",
                                    "certificate_url": "https://example.com/certificate.pdf",
                                    "name": "PC정비사",
                                    "ca": "한국산업인력공단",
                                    "cdate": "2023-07-03"
                                },
                                {
                                    "certification_id": "01RABBIT040000000000003102",
                                    "certificate_url": "https://example.com/certificate.pdf",
                                    "name": "ADSP",
                                    "ca": "한국산업인력공단",
                                    "cdate": "2023-07-03"
                                },
                                {
                                    "certification_id": "01RABBIT040000000000003103",
                                    "certificate_url": "https://example.com/certificate.pdf",
                                    "name": "SQLD",
                                    "ca": "한국산업인력공단",
                                    "cdate": "2024-06-20"
                                }
                                ],
                                "career": [],
                                "education": [
                                {
                                    "education_id": "01RABBIT050000000000003101",
                                    "school_name": "신한고등학교",
                                    "status": "GRADUATED",
                                    "major": "자연계",
                                    "start_date": "2016-03-01",
                                    "end_date": "2016-02-15",
                                    "certificate_url": "https://example.com/certificate.pdf"
                                },
                                {
                                    "education_id": "01RABBIT050000000000003102",
                                    "school_name": "서울대학교",
                                    "status": "ENROLLED",
                                    "major": "전자공학과",
                                    "start_date": "2013-03-01",
                                    "end_date": "2013-02-15",
                                    "certificate_url": "https://example.com/certificate.pdf"
                                }
                                ],
                                "ai_review": "심리학 전공과 조직개발 경험을 통해 협업과 커뮤니케이션 역량을 강화한 잠재력 높은 프론트엔드 전문가입니다."
                            },
                            "created_at": "2025-09-23T10:00:00"
                            }
                        ],
                        "pageable": {
                            "page_number": 0,
                            "page_size": 2,
                            "sort": {
                            "empty": true,
                            "unsorted": true,
                            "sorted": false
                            },
                            "offset": 0,
                            "paged": true,
                            "unpaged": false
                        },
                        "last": false,
                        "total_elements": 51,
                        "total_pages": 26,
                        "size": 2,
                        "number": 0,
                        "sort": {
                            "empty": true,
                            "unsorted": true,
                            "sorted": false
                        },
                        "first": true,
                        "number_of_elements": 2,
                        "empty": false
                        }
                    """
                )
            )
        )
    })
    ResponseEntity<Page<FetchBunnyResponse>> getBunnyList(
        @Parameter(description = "버니 목록 필터", example = "ALL, LATEST, CAPITALIZATION") 
        String filter,
        
        @ParameterObject
        @PageableDefault(page = 0, size = 15, sort = {})
        Pageable pageable
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

    // ---------------- 버니 사용자 컨텍스트 조회 ----------------
    @Operation(
        summary = "버니 사용자 컨텍스트 조회", 
        description = "특정 버니에 대한 사용자의 매수/매도 가능한 금액과 수량을 조회합니다."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "사용자 컨텍스트 조회 성공",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = BunnyUserContextResponse.class),
                examples = @ExampleObject(
                    value = """
                    {
                        "is_liked": true,
                        "buyable_amount": 5000000,
                        "sellable_quantity": 100
                    }
                    """
                )
            )
        ),
        @ApiResponse(responseCode = "404", description = "해당 버니를 찾을 수 없습니다.")
    })
    ResponseEntity<BunnyUserContextResponse> getBunnyUserContext(
        @Parameter(description = "버니 이름", example = "bunny-001", required = true)
        @AuthenticationPrincipal Jwt jwt,
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
                    "bunny_id": "01BUNNY0010000000000000074",
                    "user_name": "사용자74",
                    "user_image": "https://picsum.photos/200",
                    "user_carrot": 100000000,
                    "bunny_name": "bunny-074",
                    "bunny_type": "A",
                    "developer_type": "STABLE",
                    "position": "FULLSTACK",
                    "badges": [
                        "KAKAO",
                        "NAVER"
                    ],
                    "today_time": "2025-09-24",
                    "monthly_growth_rates": [
                        {
                        "date": "2025-05-31",
                        "closingPrice": null
                        },
                        {
                        "date": "2025-09-20",
                        "closingPrice": 118622
                        },
                        {
                        "date": "2025-09-21",
                        "closingPrice": 104783
                        },
                        {
                        "date": "2025-09-22",
                        "closingPrice": 120807
                        },
                        {
                        "date": "2025-09-23",
                        "closingPrice": 123142
                        },
                        {
                        "date": "2025-09-24",
                        "closingPrice": 133493
                        }
                    ],
                    "reliability": 14,
                    "current_price": 133493,
                    "closing_price": 127078,
                    "market_cap": 133493000,
                    "avg_bunny_type_vs_me": 14.6259,
                    "avg_position_vs_me": 25.7467,
                    "avg_dev_type_vs_me": 23.4007,
                    "competitors": [
                        {
                        "bunnyId": "01BUNNY0010000000000000031",
                        "bunnyName": "bunny-031",
                        "userImage": "https://picsum.photos/200",
                        "rank": 12,
                        "marketCap": 133940000,
                        "growthRate": 54.93
                        },
                        {
                        "bunnyId": "01BUNNY0010000000000000079",
                        "bunnyName": "bunny-079",
                        "userImage": "https://picsum.photos/200",
                        "rank": 14,
                        "marketCap": 131193000,
                        "growthRate": 22.29
                        }
                    ],
                    "my_growth_rate": 5.05,
                    "growth": 55,
                    "stability": 29,
                    "value": 23,
                    "popularity": 20,
                    "balance": 48,
                    "holder_types": [
                        {
                        "developerType": "BASIC",
                        "percentage": 35.7,
                        "count": 5
                        },
                        {
                        "developerType": "GROWTH",
                        "percentage": 7.3,
                        "count": 1
                        },
                        {
                        "developerType": "POPULAR",
                        "percentage": 13.5,
                        "count": 2
                        }
                    ],
                    "holders": [
                        {
                        "userId": "01RABBIT010000000000000008",
                        "userName": "사용자8",
                        "userImg": "https://picsum.photos/200",
                        "holdQuantity": 100
                        },
                        {
                        "userId": "01RABBIT010000000000000044",
                        "userName": "사용자44",
                        "userImg": "https://picsum.photos/200",
                        "holdQuantity": 92
                        },
                        {
                        "userId": "01RABBIT010000000000000022",
                        "userName": "사용자22",
                        "userImg": "https://picsum.photos/200",
                        "holdQuantity": 83
                        },
                        {
                        "userId": "01RABBIT010000000000000081",
                        "userName": "사용자81",
                        "userImg": "https://picsum.photos/200",
                        "holdQuantity": 79
                        },
                        {
                        "userId": "01RABBIT010000000000000039",
                        "userName": "사용자39",
                        "userImg": "https://picsum.photos/200",
                        "holdQuantity": 73
                        },
                        {
                        "userId": "01RABBIT010000000000000010",
                        "userName": "사용자10",
                        "userImg": "https://picsum.photos/200",
                        "holdQuantity": 69
                        },
                        {
                        "userId": "01RABBIT010000000000000038",
                        "userName": "사용자38",
                        "userImg": "https://picsum.photos/200",
                        "holdQuantity": 43
                        },
                        {
                        "userId": "01RABBIT010000000000000085",
                        "userName": "사용자85",
                        "userImg": "https://picsum.photos/200",
                        "holdQuantity": 26
                        }
                    ],
                    "propensity_match_rate": null,
                    "spec": {
                        "name": "사용자74",
                        "birthdate": "1990-06-27",
                        "email": "user074@google.com",
                        "phone": "010-0000-0074",
                        "image": "https://picsum.photos/200",
                        "resume": "https://example.com/resume.pdf",
                        "position": "FULLSTACK",
                        "link": [
                        {
                            "sns_id": "01RABBIT070000000000007401",
                            "url": "https://www.linkedin.com/user74",
                            "type": "LINKEDIN",
                            "favicon": "https://linkedin.com/favicon.ico"
                        }
                        ],
                        "skill": [
                        "SpringBoot",
                        "Django",
                        "Svelte",
                        "Rust"
                        ],
                        "certification": [],
                        "career": [],
                        "education": [
                            {
                                "education_id": "01RABBIT050000000000007401",
                                "school_name": "신한고등학교",
                                "status": "GRADUATED",
                                "major": "실업계",
                                "start_date": "2016-03-01",
                                "end_date": "2013-02-15",
                                "certificate_url": "https://example.com/certificate.pdf"
                            }
                        ],
                        "ai_review": "경영학 전공과 창업 경험을 토대로 사업 감각과 리더십을 겸비한 잠재력 높은 풀스택 전문가입니다."
                    },
                        "ai_review": "최신 트렌드와 프레임워크에 강해 시장의 주목을 받는 인기주 개발자",
                        "ai_feedback": "AI·ML 분야의 최신 툴셋을 빠르게 학습하여 포트폴리오에 반영하면 단가 친화형에서 성장주로 전환됩니다.",
                        "like_count": 2
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

    @Operation(summary = "거래 체결강도 top5", description = "모든 버니들의 매수/ 매도 체결강도 중 top5를 조회함")
    ResponseEntity<PressureResponse> getPressureTop5();
}
