package team.avgmax.rabbit.bunny.dto.orderBook;

import team.avgmax.rabbit.bunny.entity.Bunny;

import java.math.BigDecimal;
import java.util.List;

// OrderBookSnapshot : 프론트에서 처음 들어올 때 REST 로 한 번만
// OrderBookDiff : 주문 생성, 취소, 체결 이벤트가 일어날 때마다 WebSocket 으로 push
// Diff 규칙 : upsert(추가/수정), delete(취소)
public record OrderBookDiff(
        String bunnyName,
        List<OrderBookLevel> bidUpserts,
        List<BigDecimal> bidDeletes,
        List<OrderBookLevel> askUpserts,
        List<BigDecimal> askDeletes,
        BigDecimal currentPrice,
        long serverTime
) {}
