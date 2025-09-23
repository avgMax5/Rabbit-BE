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

    @Column(precision = 30)
    private BigDecimal holdQuantity;

    private BigDecimal costBasis;

    public static HoldBunny create(Bunny bunny, Funding funding) {
        return HoldBunny.builder()
                .bunny(bunny)
                .holder(funding.getUser())
                .holdQuantity(funding.getQuantity())
                .costBasis(bunny.getCurrentPrice().multiply(funding.getQuantity()))
                .build();
    }
}
