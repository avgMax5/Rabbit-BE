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
import java.util.ArrayList;
import java.util.List;

import team.avgmax.rabbit.user.entity.PersonalUser;

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

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private PersonalUser user;

    @Column(unique = true, nullable = false)
    private String bunnyName;

    @Enumerated(EnumType.STRING)
    private DeveloperType developerType;

    @Enumerated(EnumType.STRING)
    private BunnyType bunnyType;

    private BigDecimal reliability;

    private BigDecimal currentPrice; 

    private BigDecimal closingPrice;

    private BigDecimal marketCap;

    private int growth;

    private int stability;

    private int value;

    private int popularity;

    private int balance;
    
    private String aiReview;

    private String aiFeedback;

    private long likeCount;

    @Builder.Default
    @JoinColumn(name = "bunny_id")
    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Badge> badges = new ArrayList<>();

    public static Bunny create(FundBunny fundBunny) {
        return Bunny.builder()
                .user(fundBunny.getUser())
                .bunnyName(fundBunny.getBunnyName())
                .developerType(DeveloperType.UNDEFINED)
                .bunnyType(fundBunny.getType())
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
