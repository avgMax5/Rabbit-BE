package team.avgmax.rabbit.bunny.dto.currentPrice;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ClosingPriceUpdate(
        String bunnyName,
        BigDecimal closingPrice,
        LocalDate date
) {}
