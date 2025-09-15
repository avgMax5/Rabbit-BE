package team.avgmax.rabbit.bunny.dto.response;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import team.avgmax.rabbit.bunny.entity.BunnyHistory;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Builder
@AllArgsConstructor
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class ChartDataPoint {

    private LocalDate date;
    private BigDecimal highPrice;
    private BigDecimal lowPrice;
    private BigDecimal closingPrice;
    private BigDecimal buyQuantity;
    private BigDecimal sellQuantity;
    private BigDecimal tradeVolume; // tradeQuantity

    public static ChartDataPoint from(BunnyHistory bunnyHistory) {
        return ChartDataPoint.builder()
                .date(bunnyHistory.getDate())
                .highPrice(bunnyHistory.getHighPrice())
                .lowPrice(bunnyHistory.getLowPrice())
                .closingPrice(bunnyHistory.getClosingPrice())
                .buyQuantity(bunnyHistory.getBuyQuantity())
                .sellQuantity(bunnyHistory.getSellQuantity())
                .tradeVolume(bunnyHistory.getTradeQuantity())
                .build();
    }
}
