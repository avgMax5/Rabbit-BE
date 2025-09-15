package team.avgmax.rabbit.bunny.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import team.avgmax.rabbit.global.entity.BaseTime;
import team.avgmax.rabbit.global.util.UlidGenerator;
import team.avgmax.rabbit.bunny.entity.enums.BunnyType;
import team.avgmax.rabbit.bunny.entity.enums.DeveloperType;
import team.avgmax.rabbit.funding.entity.FundBunny;

import java.math.BigDecimal;

import team.avgmax.rabbit.user.entity.PersonalUser;
import team.avgmax.rabbit.user.entity.enums.Position;

@Entity
@Getter
@SuperBuilder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Bunny extends BaseTime {

    @Id
    @Column(name = "bunny_id", length = 26, updatable = false, nullable = false)
    @Builder.Default
    private String id = UlidGenerator.generateMonotonic();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private PersonalUser user;

    @Column(unique = true, nullable = false)
    private String bunnyName;

    @Enumerated(EnumType.STRING)
    private DeveloperType developerType;

    @Enumerated(EnumType.STRING)
    private BunnyType bunnyType;
   
    @Enumerated(EnumType.STRING)
    private Position position;

    private BigDecimal reliability;

    private BigDecimal currentPrice; 

    private BigDecimal closingPrice;

    private BigDecimal marketCap;

    // 5가지 성장 지표 추후 추가
    
    private String aiReview;

    private String aiFeedback;

    private long likeCount;

    public static Bunny create(FundBunny fundBunny) {
        return Bunny.builder()
                .user(fundBunny.getUser())
                .bunnyName(fundBunny.getBunnyName())
                .developerType(DeveloperType.GROWTH) // 추후 기본값 지정
                .bunnyType(fundBunny.getType())
                .position(fundBunny.getUser().getPosition())
                .reliability(BigDecimal.ZERO) // 추후 계산 로직 추가
                .currentPrice(fundBunny.getType().getPrice())
                .closingPrice(fundBunny.getType().getPrice())
                .marketCap(fundBunny.getType().getMarketCap())
                .aiReview("") // 추후 AI API 로직 추가
                .aiFeedback("") // 추후 AI API 로직 추가
                .build();
    }

    public void addLikeCount() {
        this.likeCount++;
    }

    public void subtractLikeCount() {
        this.likeCount--;
    }
}
