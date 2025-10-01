package team.avgmax.rabbit.user.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import team.avgmax.rabbit.bunny.entity.Bunny;
import team.avgmax.rabbit.funding.entity.Funding;
import team.avgmax.rabbit.global.entity.BaseTime;
import team.avgmax.rabbit.global.util.UlidGenerator;

import java.math.BigDecimal;

@Entity
@Getter
@SuperBuilder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class HoldBunny extends BaseTime {

    @Id
    @Column(name = "hold_bunny_id", length = 26, updatable = false, nullable = false)
    @Builder.Default
    private String id = UlidGenerator.generateMonotonic();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "holder_id", nullable = false)
    private PersonalUser holder;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bunny_id", nullable = false)
    private Bunny bunny;

    @Builder.Default
    @Column(precision = 30)
    private BigDecimal holdQuantity = BigDecimal.ZERO;

    @Builder.Default
    private BigDecimal costBasis = BigDecimal.ZERO;

    public static HoldBunny create(Bunny bunny, Funding funding) {
        return HoldBunny.builder()
                .bunny(bunny)
                .holder(funding.getUser())
                .holdQuantity(funding.getQuantity())
                .costBasis(bunny.getCurrentPrice().multiply(funding.getQuantity()))
                .build();
    }

    public static HoldBunny create(Bunny bunny, PersonalUser user) {
        return HoldBunny.builder()
                .bunny(bunny)
                .holder(user)
                .build();
    }

    public void preDecreaseQuantity(BigDecimal quantity) {
        this.holdQuantity = this.holdQuantity.subtract(quantity);
    }

    public void applySellMatch(BigDecimal amount) {
        this.costBasis = this.costBasis.subtract(amount);
    }

    public void applyBuyMatch(BigDecimal quantity, BigDecimal amount) {
        this.holdQuantity = this.holdQuantity.add(quantity);
        this.costBasis = this.costBasis.add(amount);
    }
}
