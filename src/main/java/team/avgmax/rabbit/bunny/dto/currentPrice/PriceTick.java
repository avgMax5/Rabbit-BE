package team.avgmax.rabbit.bunny.dto.currentPrice;

import java.math.BigDecimal;

public record PriceTick(
        String bunnyName,
        BigDecimal currentPrice,
        long timestamp
) {}
