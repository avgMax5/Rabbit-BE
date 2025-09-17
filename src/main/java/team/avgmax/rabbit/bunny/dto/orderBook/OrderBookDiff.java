package team.avgmax.rabbit.bunny.dto.orderBook;

import java.math.BigDecimal;
import java.util.List;

public record OrderBookDiff(
        String bunnyName,
        List<OrderBookLevel> bidUpserts,
        List<BigDecimal> bidDeletes,
        List<OrderBookLevel> askUpserts,
        List<BigDecimal> asdDeletes,
        long serverTime
) {
}
