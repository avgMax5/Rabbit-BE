package team.avgmax.rabbit.bunny.dto.response;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import team.avgmax.rabbit.bunny.entity.Badge;
import team.avgmax.rabbit.bunny.entity.Bunny;
import team.avgmax.rabbit.bunny.entity.enums.BunnyType;
import team.avgmax.rabbit.bunny.entity.enums.DeveloperType;
import team.avgmax.rabbit.user.entity.enums.Position;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
@AllArgsConstructor
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class FetchBunnyResponse {

    // 기본 정보
    private String bunnyId;
    private String userName;
    private String bunnyName;
    private DeveloperType developerType;
    private BunnyType bunnyType;
    private Position position;
    private int reliability; // 신뢰도

    // 가격 정보
    private BigDecimal currentPrice;
    private BigDecimal closingPrice;
    private BigDecimal marketCap; // 시가총액
    private BigDecimal fluctuationRate; // 등락률

    // 지표 (Entity 지표 미정)
    private int growth; // 지표 1 (성장성)
    private int stability; // 지표 2 (안정성)
    private int value; // 지표 3 (가치)
    private int popularity; // 지표 4 (인기)
    private int balance; // 지표 5 (밸런스)

    // 배지
    private List<String> badges;

    // AI Review
    private String aiReview;

    // 좋아요 수
    private long likeCount;

    // 시간
    private LocalDateTime createdAt; // 생성시간

    public static FetchBunnyResponse from(Bunny bunny) {
        return FetchBunnyResponse.builder()
                .bunnyId(bunny.getId())
                .userName(bunny.getUser().getName())
                .bunnyName(bunny.getBunnyName())
                .developerType(bunny.getDeveloperType())
                .bunnyType(bunny.getBunnyType())
                .position(bunny.getUser().getPosition())
                .reliability(bunny.getReliability())
                .currentPrice(bunny.getCurrentPrice())
                .closingPrice(bunny.getClosingPrice())
                .marketCap(bunny.getMarketCap())
                .growth(bunny.getGrowth())
                .stability(bunny.getStability())
                .value(bunny.getValue())
                .popularity(bunny.getPopularity())
                .balance(bunny.getBalance())
                .badges(bunny.getBadges().stream().map(Badge::getBadgeImg).toList())
                .aiReview(bunny.getAiReview())
                .likeCount(bunny.getLikeCount())
                .createdAt(bunny.getCreatedAt())
                .build();
    }

}
