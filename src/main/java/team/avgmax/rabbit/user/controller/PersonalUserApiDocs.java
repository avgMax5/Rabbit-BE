package team.avgmax.rabbit.user.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.jwt.Jwt;

import team.avgmax.rabbit.user.dto.request.UpdatePersonalUserRequest;
import team.avgmax.rabbit.user.dto.response.CarrotsResponse;
import team.avgmax.rabbit.user.dto.response.FetchUserResponse;
import team.avgmax.rabbit.user.dto.response.HoldBunniesResponse;
import team.avgmax.rabbit.user.dto.response.PersonalUserResponse;
import team.avgmax.rabbit.bunny.dto.response.OrderListResponse;

@Tag(name = "PersonalUser", description = "개인 사용자 API")
public interface PersonalUserApiDocs {

    @Operation(
        summary = "기본 정보 조회 (fetch)",
        description = "사용자 정보 fetch를 위해 현재 로그인한 사용자의 기본 정보를 조회합니다."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "기본 정보 조회 성공",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = FetchUserResponse.class),
                examples = @ExampleObject(
                    value = """
                    {
                        "user_id": "01HZXUSER00000000000000001",
                        "name": "사용자_01",
                        "image": "https://picsum.photos/200",
                        "role": "ROLE_USER",
                        "carrot": "1000000",
                        "my_bunny_name": "bunny-001"
                    }
                    """
                )
            )
        )
    })
    ResponseEntity<FetchUserResponse> fetchPersonalUser(
        @Parameter(description = "JWT 토큰", hidden = true) Jwt jwt
    );

    @ApiResponse(
        responseCode = "200",
        description = "나의 정보 조회 성공",
        content = @Content(
            mediaType = "application/json",
            schema = @Schema(implementation = PersonalUserResponse.class),
            examples = @ExampleObject(
                value = """
                {
                    "user_id": "01HZXUSER00000000000000001",
                    "name": "사용자_01",
                    "birthdate": "1990-01-25",
                    "image": "https://picsum.photos/200",
                    "email": "user01@test.com",
                    "resume": "https://example.com/resume.pdf",
                    "portfolio": "https://example.com/portfolio.pdf",
                    "link": [
                        {
                            "sns_id": "01HZXSNS00000000000000001",
                            "url": "https://github.com/jjweidon",
                            "type": "GITHUB",
                            "favicon": "https://github.com/favicon.ico"
                        },
                        {
                            "sns_id": "01HZXSNS00000000000000002",
                            "url": "https://instagram.com/jwoong_8",
                            "type": "INSTAGRAM",
                            "favicon": "https://instagram.com/favicon.ico"
                        },
                        {
                            "sns_id": "01HZXSNS00000000000000003",
                            "url": "https://linkedin.com/jwoong_8",
                            "type": "LINKEDIN",
                            "favicon": "https://linkedin.com/favicon.ico"
                        }
                    ],
                    "position": "BACKEND",
                    "education": [
                        {
                            "education_id": "01HZXEDUCATION00000000000000001",
                            "school_name": "신한고등학교",
                            "status": "GRADUATED",
                            "major": "자연계",
                            "start_date": "2020-09-01",
                            "end_date": "2023-06-30",
                            "certificate_url": "https://example.com/certificate.pdf"
                        },
                        {
                            "education_id": "01HZXEDUCATION00000000000000002",
                            "school_name": "신한대학교",
                            "status": "ENROLLED",
                            "major": "컴퓨터공학과",
                            "start_date": "2020-09-01",
                            "end_date": "2023-06-30",
                            "certificate_url": "https://example.com/certificate.pdf"
                        }
                    ],
                    "career": [
                        {
                            "career_id": "01HZXCAREER00000000000000001",
                            "company_name": "신한은행",
                            "status": "UNEMPLOYED",
                            "position": "마케터",
                            "start_date": "2023-09-01",
                            "end_date": "2025-12-31",
                            "certificate_url": "https://example.com/certificate.pdf"
                        },
                        {
                            "career_id": "01HZXCAREER00000000000000002",
                            "company_name": "신한DS",
                            "status": "EMPLOYED",
                            "position": "백엔드 엔지니어",
                            "start_date": "2023-09-01",
                            "end_date": "2025-12-31",
                            "certificate_url": "https://example.com/certificate.pdf"
                        }
                    ],
                    "certification": [
                        {
                            "certification_id": "01HZXCERTIFICATION00000000000000001",
                            "certificate_url": "https://fileurl.com",
                            "name": "정보처리기사",
                            "ca": "에이비지맥스",
                            "cdate": "2015-09-01"
                        },
                        {
                            "certification_id": "01HZXCERTIFICATION00000000000000002",
                            "certificate_url": "https://fileurl.com",
                            "name": "SQLD",
                            "ca": "에이비지맥스",
                            "cdate": "2015-09-02"
                        }
                    ],
                    "skill": [
                        "Java",
                        "Javascript",
                        "SpringBoot",
                        "React"
                    ]
                }
                """
            )
        )
    )
    ResponseEntity<PersonalUserResponse> getMyInfo(
        @Parameter(description = "JWT 토큰", hidden = true) Jwt jwt
    );

    @Operation(
        summary = "나의 정보 수정",
        description = "현재 로그인한 사용자의 정보를 수정합니다."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "나의 정보 수정 성공",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = PersonalUserResponse.class),
                examples = @ExampleObject(
                    value = """
                    {
                        "user_id": "01HZXUSER00000000000000001",
                        "name": "사용자_01",
                        "birthdate": "1990-01-25",
                        "image": "https://picsum.photos/200",
                        "email": "user01@test.com",
                        "resume": "https://example.com/resume.pdf",
                        "portfolio": "https://example.com/portfolio.pdf",
                        "link": [
                            {
                                "sns_id": "01HZXSNS00000000000000001",
                                "link": "https://github.com/jjweidon",
                                "type": "GITHUB",
                                "favicon": "https://github.com/favicon.ico"
                            },
                            {
                                "sns_id": "01HZXSNS00000000000000002",
                                "link": "https:/instagram.com/jwoong_8",
                                "type": "INSTAGRAM",
                                "favicon": "https:/instagram.com/favicon.ico"
                            },
                            {
                                "sns_id": "01HZXSNS00000000000000003",
                                "link": "https:/linkedin.com/jwoong_8",
                                "type": "LINKEDIN",
                                "favicon": "https:/linkedin.com/favicon.ico"
                            }
                        ],
                        "position": "BACKEND",
                        "education": [
                            {   
                                "education_id": "01HZXEDUCATION00000000000000001",
                                "school_name": "신한고등학교",
                                "status": "GRADUATED",
                                "major": "자연계",
                                "start_date": "2020-09-01",
                                "end_date": "2023-06-30",
                                "certificate_url": "https://example.com/certificate.pdf"
                            },
                            {
                                "education_id": "01HZXEDUCATION00000000000000002",
                                "school_name": "신한대학교",
                                "status": "ENROLLED",
                                "major": "컴퓨터공학과",
                                "start_date": "2020-09-01",
                                "end_date": "2023-06-30",
                                "certificate_url": "https://example.com/certificate.pdf"
                            }
                        ],
                        "career": [
                            {
                                "career_id": "01HZXCAREER00000000000000001",
                                "company_name": "신한은행",
                                "status": "UNEMPLOYED",
                                "position": "마케터", 
                                "start_date": "2023-09-01",
                                "end_date": "2025-12-31",
                                "certificate_url": "https://example.com/certificate.pdf"
                            },
                            {
                                "career_id": "01HZXCAREER00000000000000002",
                                "company_name": "신한DS",
                                "status": "EMPLOYED",
                                "position": "백엔드 엔지니어",
                                "start_date": "2023-09-01",
                                "end_date": "2025-12-31",
                                "certificate_url": "https://example.com/certificate.pdf"
                            }
                        ],
                        "certification": [
                            {
                                "certification_id": "01HZXCERTIFICATION00000000000000001",
                                "certificate_url": "https://fileurl.com",
                                "name": "정보처리기사",
                                "ca": "에이비지맥스",
                                "cdate": "2015-09-01"
                            },
                            {
                                "certification_id": "01HZXCERTIFICATION00000000000000002",
                                "certificate_url": "https://fileurl.com",
                                "name": "SQLD",
                                "ca": "에이비지맥스",
                                "cdate": "2015-09-02"
                            }
                        ],
                        "skill": [
                            "Java",
                            "Javascript",
                            "SpringBoot",
                            "React"
                        ]
                    }
                    """
                )
            )
        )
    })
    ResponseEntity<PersonalUserResponse> updateMyInfo(
        @Parameter(description = "JWT 토큰", hidden = true) Jwt jwt,
        @Parameter(description = "사용자 정보 수정 요청", required = true) UpdatePersonalUserRequest request
    );

    @Operation(
        summary = "보유 캐럿 조회",
        description = "현재 로그인한 사용자가 보유한 캐럿 정보를 조회합니다."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "보유 캐럿 조회 성공",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = CarrotsResponse.class),
                examples = @ExampleObject(
                    value = """
                    {
                        "carrots": 1000000
                    }
                    """
                )
            )
        )
    })
    ResponseEntity<CarrotsResponse> getMyCarrots(
        @Parameter(description = "JWT 토큰", hidden = true) Jwt jwt
    );

    @Operation(
        summary = "보유 버니 조회",
        description = "현재 로그인한 사용자가 보유한 버니 정보를 조회합니다."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "보유 버니 조회 성공",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = HoldBunniesResponse.class),
                examples = @ExampleObject(
                    value = """
                    {
                        "hold_bunnies": [
                            {
                                "bunny_id": "01HZXBUNNY00000000000000001",
                                "bunny_name": "bunny-001",
                                "hold_quantity": 100,
                                "total_buy_amount": 100000
                            },
                            {
                                "bunny_id": "01HZXBUNNY00000000000000002",
                                "bunny_name": "bunny-002",
                                "hold_quantity": 50,
                                "total_buy_amount": 100000
                            }
                        ]
                    }
                    """
                )
            )
        )
    })
    ResponseEntity<HoldBunniesResponse> getMyHoldBunnies(
        @Parameter(description = "JWT 토큰", hidden = true) Jwt jwt
    );

    @Operation(
        summary = "미체결 주문 목록 조회",
        description = "현재 로그인한 사용자의 미체결 주문 내역을 조회합니다."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "미체결 주문 목록 조회 성공",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = OrderListResponse.class),
                examples = @ExampleObject(
                    value = """
                    {
                        "orders": [
                            {
                                "order_id": "01HZXORDER00000000000000001",
                                "bunny_name": "bunny-001",
                                "bunny_id": "01HZXBUNNY00000000000000001",
                                "quantity": 100,
                                "unit_price": 1000,
                                "order_type": "BUY"
                            },
                            {
                                "order_id": "01HZXORDER00000000000000002",
                                "bunny_name": "bunny-002",
                                "bunny_id": "01HZXBUNNY00000000000000002",
                                "quantity": 50,
                                "unit_price": 2000,
                                "order_type": "SELL"
                            }
                        ]
                    }
                    """
                )
            )
        )
    })
    ResponseEntity<OrderListResponse> getMyOrders(
        @Parameter(description = "JWT 토큰", hidden = true) Jwt jwt
    );
}
