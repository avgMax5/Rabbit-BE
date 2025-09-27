package team.avgmax.rabbit.bunny.service.match;

import java.math.BigDecimal;
import java.util.Set;

public record MatchingResult(
        Set<BigDecimal> touchedBid,
        Set<BigDecimal> touchedAsk,
        BigDecimal lastTradePrice
) {}
