package team.avgmax.rabbit.bunny.dto.orderBook;

import team.avgmax.rabbit.bunny.entity.Bunny;

import java.math.BigDecimal;
import java.util.List;

public record OrderBookSnapshot(
        String bunnyName,
        List<OrderBookLevel> bids,  // 매수 레벨 목록
        List<OrderBookLevel> asks,  // 매도 레벨 목록
        BigDecimal currentPrice,    // 현재 기준 단가 (프론트에서 계산 : ((호가 - 현재가) / 현재가 * 100)
        long serverTime             // 서버 기준 시각(밀리초) → 프론트 동기화/지연 보정 참고용
) {
    public static OrderBookSnapshot from(
            Bunny bunny,
            List<OrderBookLevel> bids,
            List<OrderBookLevel> asks,
            BigDecimal currentPrice
    ) {
        return new OrderBookSnapshot(
                bunny.getBunnyName(),
                bids,
                asks,
                currentPrice,
                System.currentTimeMillis()
        );
    }
}
