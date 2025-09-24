package team.avgmax.rabbit.bunny.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import team.avgmax.rabbit.bunny.entity.id.BunnyHistoryId;
import team.avgmax.rabbit.global.entity.BaseTime;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Getter
@SuperBuilder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@IdClass(BunnyHistoryId.class)
public class BunnyHistory extends BaseTime {

    @Id
    @Column(name = "date", nullable = false)
    private LocalDate date;
    
    @Id
    @Column(name = "bunny_id", length = 26, nullable = false)
    private String bunnyId;

    private BigDecimal closingPrice;

    private BigDecimal highPrice;

    private BigDecimal lowPrice;

    private BigDecimal buyQuantity;    // tradeQuantity + 자정 시점 BUY 오픈 잔량

    private BigDecimal sellQuantity;   // tradeQuantity + 자정 시점 SELL 오픈 잔량

    private BigDecimal tradeQuantity;  // Match 의 quantity

    private BigDecimal marketCap;
}
