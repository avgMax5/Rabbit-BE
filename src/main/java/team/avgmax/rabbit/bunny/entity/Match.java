package team.avgmax.rabbit.bunny.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import team.avgmax.rabbit.bunny.entity.enums.OrderType;
import team.avgmax.rabbit.global.entity.BaseTime;
import team.avgmax.rabbit.global.util.UlidGenerator;

import java.math.BigDecimal;

import team.avgmax.rabbit.user.entity.PersonalUser;

@Entity
@Getter
@SuperBuilder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "`match`")
public class Match extends BaseTime {

    @Id
    @Column(name = "match_id", length = 26, updatable = false, nullable = false)
    @Builder.Default
    private String id = UlidGenerator.generateMonotonic();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bunny_id", nullable = false)
    private Bunny bunny;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sell_user_id", nullable = false)
    private PersonalUser sellUser;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "buy_user_id", nullable = false)
    private PersonalUser buyUser;

    private BigDecimal quantity;

    private BigDecimal unitPrice;

    public static Match create(Bunny bunny, Order myOrder, Order counter, BigDecimal tradableQuantity, BigDecimal tradePrice) {
        PersonalUser buyUser = myOrder.getOrderType() == OrderType.BUY ? myOrder.getUser() : counter.getUser();
        PersonalUser sellUser = myOrder.getOrderType() == OrderType.SELL ? myOrder.getUser() : counter.getUser();

        return Match.builder()
                .bunny(bunny)
                .buyUser(buyUser)
                .sellUser(sellUser)
                .quantity(tradableQuantity)
                .unitPrice(tradePrice)
                .build();
    }
}
