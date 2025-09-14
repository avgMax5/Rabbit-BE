package team.avgmax.rabbit.funding.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import team.avgmax.rabbit.funding.dto.request.CreateFundBunnyRequest;
import team.avgmax.rabbit.global.entity.BaseTime;
import team.avgmax.rabbit.global.util.UlidGenerator;
import team.avgmax.rabbit.user.entity.PersonalUser;
import team.avgmax.rabbit.bunny.entity.enums.BunnyType;
import team.avgmax.rabbit.bunny.entity.Bunny;
import team.avgmax.rabbit.user.entity.HoldBunny;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Getter
@SuperBuilder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class FundBunny extends BaseTime {

    @Id
    @Column(name = "fund_bunny_id", length = 26, updatable = false, nullable = false)
    @Builder.Default
    private String id = UlidGenerator.generateMonotonic();

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private PersonalUser user;

    @Column(unique = true, nullable = false)
    private String bunnyName;

    @Enumerated(EnumType.STRING)
    private BunnyType type;

    @Builder.Default
    @Column(precision = 30)
    private BigDecimal collectedBny = BigDecimal.ZERO;

    private LocalDateTime endAt;

    @OneToMany(mappedBy = "fundBunny", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Funding> fundings;

    public static FundBunny create(CreateFundBunnyRequest request, PersonalUser user, long expiryMillis) {
        return FundBunny.builder()
                .user(user)
                .bunnyName(request.bunnyName())
                .type(request.bunnyType())
                .endAt(LocalDateTime.now().plusDays(expiryMillis / (24 * 60 * 60 * 1000)))
                .build();
    }

    public void addBny(BigDecimal bny) {
        this.collectedBny = this.collectedBny.add(bny);
    }

    public boolean isReadyForListing() {
        return this.collectedBny.compareTo(this.type.getTotalSupply()) >= 0;
    }

    public Bunny convertToBunny() {
        return Bunny.create(this);
    }

    public List<HoldBunny> createHoldBunnies(Bunny bunny, List<Funding> fundings) {
        return fundings.stream()
                .map(funding -> funding.convertToHoldBunny(bunny))
                .toList();
    }
}
