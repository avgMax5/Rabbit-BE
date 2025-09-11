package team.avgmax.rabbit.bunny.dto.data;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Builder
public class DailyPriceData {
    private LocalDate date;
    private BigDecimal closingPrice;
}
