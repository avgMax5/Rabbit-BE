package team.avgmax.rabbit.bunny.dto.response;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.Builder;
import lombok.Getter;
import team.avgmax.rabbit.bunny.dto.data.ComparisonData;
import team.avgmax.rabbit.bunny.dto.data.DailyPriceData;
import team.avgmax.rabbit.bunny.dto.data.MyBunnyByDevTypeData;
import team.avgmax.rabbit.bunny.dto.data.MyBunnyByHolderData;
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

    // 코인 정보
    private String bunnyId;
    private String userName;
    private String userImage;
    private BigDecimal userCarrot;
    private String bunnyName;
    private BunnyType bunnyType;                 // 버니 유형 (희소 자산 / 밸런스 / 단가 친화)
    private DeveloperType developerType;         // 개발자 유형 5가지
    private Position position;                   // 직군
    private List<String> badges;
    private LocalDate todayTime;

    // 성장성 (차트)
    private List<DailyPriceData> monthlyGrowthRates;   // 전월말 ~ 금월말 성장률
    private List<DailyPriceData> priceHistory;

    // 시장 신뢰도
    private int reliability;

    private BigDecimal currentPrice;
    private BigDecimal closingPrice;
    private BigDecimal marketCap;                   // 시가 총액

    // 비교 지표
    private BigDecimal avgBunnyTypeVsMe;            // 버니 유형별 성장률 비교
    private BigDecimal avgPositionVsMe;             // 직군별 성장률 비교
    private BigDecimal avgDevTypeVsMe;              // 개발자 유형별 성장률 비교
    private List<ComparisonData> competitors;       // 경쟁자와 비교 (나와 가까운 순위 위아래)
    private BigDecimal myGrowthRate;

    // 개발자 유형 지표
    private int growth;
    private int stability;
    private int value;
    private int popularity;
    private int balance;

    // 내 코인을 보유한 유형 및 보유자 확인
    private List<MyBunnyByDevTypeData> holderTypes;
    private List<MyBunnyByHolderData> holders;

    // 나의 스펙 정보
    private SpecResponse spec;

    // AI 개선 제안
    private String aiReview;
    private String aiFeedback;

    private long likeCount;                     // 좋아요 수
}
