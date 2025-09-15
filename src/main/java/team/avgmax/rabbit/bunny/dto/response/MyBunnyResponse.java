package team.avgmax.rabbit.bunny.dto.response;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.Builder;
import lombok.Getter;
import team.avgmax.rabbit.bunny.dto.data.ComparisonData;
import team.avgmax.rabbit.bunny.dto.data.DailyPriceData;
import team.avgmax.rabbit.bunny.dto.data.MyBunnyByDevTypeData;
import team.avgmax.rabbit.bunny.dto.data.MyBunnyByHolderData;
import team.avgmax.rabbit.bunny.entity.Badge;
import team.avgmax.rabbit.bunny.entity.enums.BunnyType;
import team.avgmax.rabbit.bunny.entity.enums.DeveloperType;
import team.avgmax.rabbit.user.dto.response.SpecResponse;
import team.avgmax.rabbit.user.entity.enums.Position;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Getter
@Builder
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class MyBunnyResponse {

    // 요구사항 1: 코인명, 코인유형, 직군, 이름, 오늘날짜, 배지
    private String bunnyId;
    private String userName; // 이름
    private String userImage;
    private BigDecimal userCarrot; // 보유 캐럿
    private String bunnyName; // 코인명
    private BunnyType bunnyType; // 버니 유형 (희소, 밸런스, 단가)
    private DeveloperType developerType; //  개발자 유형
    private Position position; // 직군
    private List<Badge> badges; // 배지
    private LocalDate todayTime; // 오늘 날짜

    // 요구사항 2: 성장성(차트)
    private List<DailyPriceData> monthlyGrowthRates; // 전월말 ~ 이번월말 성장률
    private List<DailyPriceData> priceHistory;

    // 요구사항 3: 시장 신뢰도
    private BigDecimal reliability;

    // 요구사항 4: 시가총액, 현재가, 종가
    private BigDecimal currentPrice;
    private BigDecimal closingPrice;
    private BigDecimal marketCap; // 시가 총액

    // 요구사항 5: 삭제

    // 요구사항 6: 경쟁 / 비교 지표
    private BigDecimal avgBunnyTypeVsMe; // 버니 유형별 성장률 비교
    private BigDecimal avgPositionVsMe; // 직군별 성장률 비교
    private BigDecimal avgDevTypeVsMe; // 개발자 유형별 성장률 비교
    private List<ComparisonData> competitors; // 경쟁자와 비교 (나와 가까운 순위 위아래)
    private BigDecimal myGrowthRate;

    // 요구사항 7: 개발자 유형 지표
    private BigDecimal indicator1;
    private BigDecimal indicator2;
    private BigDecimal indicator3;
    private BigDecimal indicator4;
    private BigDecimal indicator5;

    // 요구사항 9: 내 코인을 보유한 유형 및 보유자 확인
    private List<MyBunnyByDevTypeData> holderTypes;
    private List<MyBunnyByHolderData> holders;

    // 요구사항 11: 성향 일치율 (거래 데이터 + 실제 성향)
    private BigDecimal propensityMatchRate;

    // 요구사항 12 : 나의 정보 + 스펙정보
    private SpecResponse spec;

    // 요구사항 13 : AI 개선 제안
    private String aiReview;
    private String aiFeedback;
}
