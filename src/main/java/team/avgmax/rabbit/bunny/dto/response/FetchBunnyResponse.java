package team.avgmax.rabbit.bunny.dto.response;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import team.avgmax.rabbit.bunny.entity.Badge;
import team.avgmax.rabbit.bunny.entity.Bunny;
import team.avgmax.rabbit.bunny.entity.enums.BadgeImg;
import team.avgmax.rabbit.bunny.entity.enums.BunnyType;
import team.avgmax.rabbit.bunny.entity.enums.DeveloperType;
import team.avgmax.rabbit.user.entity.enums.Position;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

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
    private BigDecimal reliability; // 신뢰도

    // 가격 정보
    private BigDecimal currentPrice;
    private BigDecimal closingPrice;
    private BigDecimal marketCap; // 시가총액
    private BigDecimal fluctuationRate; // 등락률

    // 지표 (Entity 지표 미정)
    private BigDecimal Indicator1; // 지표 1 (성장형 이었던 것)
    private BigDecimal Indicator2; // 지표 2 (안정형)
    private BigDecimal Indicator3; // 지표 3 (가치형)
    private BigDecimal Indicator4; // 지표 4 (인기형)
    private BigDecimal Indicator5; // 지표 5 (밸런스형)

    // 배지
    private List<BadgeImg> badges;

    // 시간
    private LocalDateTime createdAt; // 생성시간

    public static FetchBunnyResponse from(Bunny bunny, List<Badge> badges) {
        List<BadgeImg> badgeImgs = (badges != null)
                ? badges.stream().map(Badge::getBadgeImg).collect(Collectors.toList())
                : Collections.emptyList(); // NullPointException 을 대비하면서 null 이면 빈 list 를 사용할 수 있도록 함.

        return FetchBunnyResponse.builder()
                .bunnyId(bunny.getId())
                .userName(bunny.getUser().getName())
                .bunnyName(bunny.getBunnyName())
                .developerType(bunny.getDeveloperType())
                .bunnyType(bunny.getBunnyType())
                .position(bunny.getPosition())
                .reliability(bunny.getReliability())
                .currentPrice(bunny.getCurrentPrice())
                .closingPrice(bunny.getClosingPrice())
                .marketCap(bunny.getMarketCap())
//                .Indicator1(bunny.)
//                .Indicator2(bunny.)
//                .Indicator3(bunny.)
//                .Indicator4(bunny.)
//                .Indicator5(bunny.)
                .badges(badgeImgs)
                .createdAt(bunny.getCreatedAt())
                .build();
    }

}
