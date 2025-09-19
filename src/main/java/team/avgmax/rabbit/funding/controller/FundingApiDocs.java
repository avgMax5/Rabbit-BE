package team.avgmax.rabbit.funding.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.jwt.Jwt;
import team.avgmax.rabbit.funding.dto.request.CreateFundBunnyRequest;
import team.avgmax.rabbit.funding.dto.request.CreateFundingRequest;
import team.avgmax.rabbit.funding.dto.response.FundBunnyCountResponse;
import team.avgmax.rabbit.funding.dto.response.FundBunnyDetailResponse;
import team.avgmax.rabbit.funding.dto.response.FundBunnyListResponse;
import team.avgmax.rabbit.funding.dto.response.FundBunnyResponse;
import team.avgmax.rabbit.funding.dto.response.FundingResponse;

@Tag(name = "Funding", description = "펀딩 API")
public interface FundingApiDocs {

    @Operation(
        summary = "펀딩 버니 수 현황 조회",
        description = "전체 펀딩 버니의 수 현황을 조회합니다."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "펀딩 버니 수 현황 조회 성공",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = FundBunnyCountResponse.class),
                examples = @ExampleObject(
                    value = """
                    {
                        "listed_bunny_count": 15,
                        "fund_bunny_count": 8,
                        "closing_soon_bunny_count": 3
                    }
                    """
                )
            )
        )
    })
    ResponseEntity<FundBunnyCountResponse> getFundBunnyCount();

    @Operation(
        summary = "버니 이름 중복 체크",
        description = "펀딩 버니 이름의 중복 여부를 확인합니다."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "사용 가능한 이름"
        ),
        @ApiResponse(
            responseCode = "409",
            description = "중복된 이름"
        )
    })
    ResponseEntity<Void> checkBunnyName(
        @Parameter(description = "JWT 토큰", hidden = true) Jwt jwt,
        @Parameter(description = "체크할 버니 이름", required = true) String bunnyName
    );

    @Operation(
        summary = "버니 심사 요청",
        description = "새로운 펀딩 버니 심사를 요청합니다."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "버니 심사 요청 성공",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = FundBunnyResponse.class),
                examples = @ExampleObject(
                    value = """
                    {
                        "fund_bunny_id": "01HZXYBUNNY000000000000001",
                        "bunny_name": "bunny-001",
                        "bunny_type": "A",
                        "target_bny": 1000,
                        "collected_bny": 21,
                        "remaining_bny": 979,
                        "created_at": "2024-01-15T10:30:00",
                        "end_at": "2024-02-15T10:30:00"
                    }
                    """
                )
            )
        )
    })
    ResponseEntity<FundBunnyResponse> createFundBunnies(
        @Parameter(description = "JWT 토큰", hidden = true) Jwt jwt,
        @Parameter(description = "펀딩 버니 생성 요청 정보", required = true) CreateFundBunnyRequest request
    );

    @Operation(
        summary = "펀딩 중인 버니 목록 조회",
        description = "펀딩 중인 버니 목록을 정렬 옵션에 따라 조회합니다."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "펀딩 중인 버니 목록 조회 성공",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = FundBunnyListResponse.class),
                examples = @ExampleObject(
                    value = """
                    {
                        "size": 2,
                        "fund_bunnies": [
                            {
                                "fund_bunny_id": "01HZXYBUNNY000000000000001",
                                "bunny_name": "bunny-001",
                                "bunny_type": "A",
                                "target_bny": 1000,
                                "collected_bny": 21,
                                "remaining_bny": 979,
                                "created_at": "2024-01-15T10:30:00",
                                "end_at": "2024-02-15T10:30:00"
                            },
                            {
                                "fund_bunny_id": "01HZXYBUNNY000000000000002",
                                "bunny_name": "bunny-002",
                                "bunny_type": "B",
                                "target_bny": 100000,
                                "collected_bny": 50001,
                                "remaining_bny": 49999,
                                "created_at": "2024-01-10T09:15:00",
                                "end_at": "2024-02-10T09:15:00"
                            },
                            {
                                "fund_bunny_id": "01HZXYBUNNY000000000000003",
                                "bunny_name": "bunny-003",
                                "bunny_type": "C",
                                "target_bny": 1000000,
                                "collected_bny": 5002004,
                                "remaining_bny": 4997996,
                                "created_at": "2024-01-10T09:10:00",
                                "end_at": "2024-02-10T09:10:00"
                            }
                        ]
                    }
                    """
                )
            )
        )
    })
    ResponseEntity<FundBunnyListResponse> getFundBunnyList(
        @Parameter(description = "정렬 타입 (newest, oldest, mostInvested, leastInvested)") String sortType,
        @Parameter(description = "페이징 정보 (기본값: page=0, size=15)") Pageable pageable
    );

    @Operation(
        summary = "펀딩 중인 버니 상세 조회",
        description = "특정 펀딩 버니의 상세 정보를 조회합니다."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "펀딩 중인 버니 상세 조회 성공",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = FundBunnyDetailResponse.class),
                examples = @ExampleObject(
                    value = """
                    {
                        "fund_bunny_id": "01HZXYBUNNY000000000000001",
                        "bunny_name": "bunny-001",
                        "bunny_type": "A",
                        "target_bny": 1000,
                        "collected_bny": 21,
                        "available_bny": 480,
                        "my_holding_quantity": 120,
                        "holding_status": {
                            "top1": 38.1,
                            "top2": 30.2,
                            "top3": 20.1,
                            "others": 10.31,
                            "remaining": 1.29
                        },
                        "my_account_bny": 1100000,
                        "my_account_c": 233000000,
                        "spec": {
                            "name": "정재웅",
                            "email": "jungjaewoong@example.com",
                            "phone": "010-1234-5678",
                            "image": "https://picsum.photos/200",
                            "resume": "https://resumeurl",
                            "position": "BACKEND",
                            "link": [
                                {
                                    "sns_id": "01HZXSNS00000000000000001",
                                    "url": "https://github.com/example",
                                    "type": "GITHUB",
                                    "favicon": "https://github.com/favicon.ico"
                                },
                                {
                                    "sns_id": "01HZXSNS00000000000000002",
                                    "url": "https://linkedin.com/in/example",
                                    "type": "LINKEDIN",
                                    "favicon": "https://linkedin.com/favicon.ico"
                                }
                            ],
                            "skill": [
                                "Java",
                                "SpringBoot"
                            ],
                            "certification": [
                                {
                                    "certification_id": "01HZXCERT0000000000000001",
                                    "certificate_url": "https://certificationurl1",
                                    "name": "정보처리기사",
                                    "ca": "에이비지맥스",
                                    "cdate": "2015-09-01"
                                },
                                {
                                    "certification_id": "01HZXCERT0000000000000002",
                                    "certificate_url": "https://certificationurl2",
                                    "name": "SQLD",
                                    "ca": "에이비지맥스",
                                    "cdate": "2015-09-02"
                                }
                            ],
                            "career": [
                                {
                                    "career_id": "01HZXCAREER00000000000001",
                                    "company_name": "에이비지맥스",
                                    "status": "EMPLOYED",
                                    "position": "백엔드 개발자",
                                    "start_date": "2021-01-01",
                                    "end_date": null,
                                    "certificate_url": "https://companycerturl.com/career1"
                                },
                                {
                                    "career_id": "01HZXCAREER00000000000002",
                                    "company_name": "테크스타트업",
                                    "status": "UNEMPLOYED",
                                    "position": "주니어 개발자",
                                    "start_date": "2019-07-01",
                                    "end_date": "2020-12-31",
                                    "certificate_url": "https://companycerturl.com/career2"
                                }
                            ],
                            "education": [
                                {
                                    "education_id": "01HZXEDU00000000000000001",
                                    "school_name": "서울대학교",
                                    "status": "ENROLLED",
                                    "major": "컴퓨터공학과",
                                    "start_date": "2015-03-01",
                                    "end_date": "2019-02-28",
                                    "certificate_url": "https://eduurl.com/snu"
                                },
                                {
                                    "education_id": "01HZXEDU00000000000000002",
                                    "school_name": "서울고등학교",
                                    "status": "GRADUATED",
                                    "major": "자연계열",
                                    "start_date": "2012-03-01",
                                    "end_date": "2015-02-28",
                                    "certificate_url": "https://eduurl.com/seoulhs"
                                }
                            ],
                            "ai_review": "이 개발자는 백엔드 개발 경험이 풍부하고 Java, SpringBoot 기술 스택에 능숙합니다. 정보처리기사와 SQLD 자격증을 보유하고 있어 데이터베이스 관련 업무에도 전문성을 가지고 있습니다."
                        }
                    }
                    """
                )
            )
        )
    })
    ResponseEntity<FundBunnyDetailResponse> getFundBunnyDetail(
        @Parameter(description = "JWT 토큰", hidden = true) Jwt jwt,
        @Parameter(description = "펀딩 버니 ID", required = true) String fundBunnyId
    );

    @Operation(
        summary = "펀딩 등록",
        description = "특정 펀딩 버니에 대한 펀딩을 등록합니다."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "펀딩 등록 성공",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = FundingResponse.class),
                examples = @ExampleObject(
                    value = """
                    {
                        "funding_id": "01HZXYFUNDING000000000000001",
                        "fund_bunny_id": "01HZXYBUNNY000000000000001",
                        "user_id": "01HZXUSER00000000000000001",
                        "quantity": 21,
                        "my_holding_quantity": 120
                    }
                    """
                )
            )
        ),
        @ApiResponse(
            responseCode = "204",
            description = "펀딩 등록 성공 및 버니 상장 완료",
            content = @Content
        )
    })
    ResponseEntity<FundingResponse> postFunding(
        @Parameter(description = "JWT 토큰", hidden = true) Jwt jwt,
        @Parameter(description = "펀딩 버니 ID", required = true) String fundBunnyId,
        @Parameter(description = "펀딩 등록 요청 정보", required = true) CreateFundingRequest request
    );
}
