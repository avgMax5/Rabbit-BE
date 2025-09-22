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
import java.util.stream.IntStream;
import java.time.LocalDateTime;

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

    @Builder.Default
    private int reliability = 10;

    private BigDecimal currentPrice; 

    private BigDecimal closingPrice;

    private BigDecimal marketCap;

    @Builder.Default
    private int growth = 10;

    @Builder.Default
    private int stability = 10;

    @Builder.Default
    private int value = 10;

    @Builder.Default
    private int popularity = 10;

    @Builder.Default
    private int balance = 10;
    
    @Builder.Default
    private String aiReview = "";

    @Lob
    @Builder.Default
    @Basic(fetch = FetchType.LAZY)
    private String aiFeedback = "";

    private long likeCount;

    @Builder.Default
    @JoinColumn(name = "bunny_id")
    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Badge> badges = new ArrayList<>();

    public static Bunny create(FundBunny fundBunny) {
        return Bunny.builder()
                .user(fundBunny.getUser())
                .bunnyName(fundBunny.getBunnyName())
                .developerType(DeveloperType.BASIC)
                .bunnyType(fundBunny.getType())
                .currentPrice(fundBunny.getType().getPrice())
                .closingPrice(fundBunny.getType().getPrice())
                .marketCap(fundBunny.getType().getMarketCap())
                .build();
    }

    public void addLikeCount() {
        this.likeCount++;
    }

    public void subtractLikeCount() {
        this.likeCount--;
    }

    public void updateCurrentPrice(BigDecimal price) {
        if (price == null || price.signum() < 0) {
            throw new IllegalArgumentException("invalid price");
        }
        this.currentPrice = price;
    }

    public void updateReliability(double reliability) {
        this.reliability = (int) reliability;
    }

    public void updateGrowth(double growth) {
        this.growth = (int) growth;
        updateDeveloperType();
    }

    public void updateStability(double stability) {
        this.stability = (int) stability;
        updateDeveloperType();
    }

    public void updateValue(double value) {
        this.value = (int) value;
        updateDeveloperType();
    }

    public void updatePopularity(double popularity) {
        this.popularity = (int) popularity;
        updateDeveloperType();
    }

    public void updateBalance(double balance) {
        this.balance = (int) balance;
        updateDeveloperType();
    }

    private void updateDeveloperType() {
        // 생성 후 7일 이내는 BASIC 유지
        if (this.getCreatedAt().isAfter(LocalDateTime.now().minusDays(7))) {
            this.developerType = DeveloperType.BASIC;
            return;
        }

        int maxScore = IntStream.of(growth, stability, value, popularity, balance).max().orElseThrow();
        if (maxScore == growth) {
            this.developerType = DeveloperType.GROWTH;
        } else if (maxScore == stability) {
            this.developerType = DeveloperType.STABLE;
        } else if (maxScore == value) {
            this.developerType = DeveloperType.VALUE;
        } else if (maxScore == popularity) {
            this.developerType = DeveloperType.POPULAR;
        } else if (maxScore == balance) {
            this.developerType = DeveloperType.BALANCE;
        } else {
            this.developerType = DeveloperType.BASIC; // 기본값
        }
    }
    
    public void updateAiReviewAndFeedback(String aiReview, String aiFeedback) {
        this.aiReview = aiReview;
        this.aiFeedback = aiFeedback;
    }
}
