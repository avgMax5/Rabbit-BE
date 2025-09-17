package team.avgmax.rabbit.bunny.dto.orderBook;

import java.math.BigDecimal;
import java.util.List;

public record OrderBookSnapshot(
        String bunnyName,
        List<OrderBookLevel> bids,  // 매수 레벨 목록 (가격 내림차순, 같은 가격이면 firstIn 오름차순)
        List<OrderBookLevel> asks,  // 매도 레벨 목록 (가격 오름차순, 같은 가격이면 firstIn 오름차순)
        BigDecimal currentPrice,    // 현재 기준 단가 (프론트에서 계산 : ((호가 - 현재가) / 현재가 * 100)
        long serverTime             // 서버 기준 시각(밀리초) → 프론트 동기화/지연 보정 참고용
) {
}
