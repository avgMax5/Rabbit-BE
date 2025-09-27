package team.avgmax.rabbit.bunny.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import team.avgmax.rabbit.bunny.dto.data.BunnyPressureData;
import team.avgmax.rabbit.bunny.dto.response.PressureResponse;
import team.avgmax.rabbit.bunny.entity.Bunny;
import team.avgmax.rabbit.bunny.entity.BunnyHistory;
import team.avgmax.rabbit.bunny.entity.enums.OrderType;
import team.avgmax.rabbit.bunny.repository.BunnyHistoryRepository;
import team.avgmax.rabbit.bunny.repository.BunnyRepository;
import team.avgmax.rabbit.bunny.repository.OrderRepository;
import team.avgmax.rabbit.bunny.repository.custom.MatchDailyAggregateRepository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class BunnyHistoryService {

    private final BunnyRepository bunnyRepository;
    private final BunnyHistoryRepository bunnyHistoryRepository;
    private final MatchDailyAggregateRepository aggregateRepository;
    private final OrderRepository orderRepository;

    private final BunnyIndicatorService bunnyIndicatorService;

    @Transactional
    public void aggregateFor(final LocalDate targetDate) {

        // 집계 구간 계산
        final LocalDateTime from = targetDate.atStartOfDay();
        final LocalDateTime to = targetDate.plusDays(1).atStartOfDay();

        log.info("BunnyHistoryService: targetDate={}, window={}~{}", targetDate, from, to);

        // Bunny 캐시
        final List<Bunny> bunnies = bunnyRepository.findAll();
        if (bunnies.isEmpty()) return;

        final Map<String, Bunny> bunnyMap = bunnies.stream().collect(Collectors.toMap(Bunny::getId, Function.identity()));

        // 하루치 체결 집계 (Match 기반)
        final List<MatchDailyAggregateRepository.Row> rows = aggregateRepository.aggregateByBunny(from, to);
        if (rows == null || rows.isEmpty()) return;

        // 집계 결과 → BunnyHistory upsert(멱등)
        for (MatchDailyAggregateRepository.Row row : rows) {
            final String bunnyId = row.bunnyId();
            final Bunny bunny = bunnyMap.get(bunnyId);
            if (bunny == null) continue;

            // 종가
            final BigDecimal closingPrice = row.closingPrice();
            if (closingPrice == null) continue;

            // 체결 기반 지표
            final BigDecimal highPrice = row.highPrice() != null ? row.highPrice() : BigDecimal.ZERO;
            final BigDecimal lowPrice = row.lowPrice() != null ? row.lowPrice() : BigDecimal.ZERO;
            final BigDecimal tradeQuantity = row.tradeQuantity() != null ? row.tradeQuantity() : BigDecimal.ZERO;

            // 자정 시점 오픈 잔량 합산 (BUY/SELL)
            final BigDecimal openBuyQty = orderRepository.sumOpenQuantityByBunnyAndSide(bunnyId, OrderType.BUY);
            final BigDecimal openSellQty = orderRepository.sumOpenQuantityByBunnyAndSide(bunnyId, OrderType.SELL);

            final BigDecimal buyQuantity = tradeQuantity.add(openBuyQty);    // 총 매수 주문량
            final BigDecimal sellQuantity = tradeQuantity.add(openSellQty);  // 총 매도 주문량

            // 시가총액 = 종가 * 발행량
            final BigDecimal totalSupply =
                    (bunny.getBunnyType() != null && bunny.getBunnyType().getTotalSupply() != null)
                            ? bunny.getBunnyType().getTotalSupply()
                            : BigDecimal.ZERO;
            final BigDecimal marketCap = closingPrice.multiply(totalSupply);

            final BunnyHistory history = BunnyHistory.builder()
                    .date(targetDate)
                    .bunnyId(bunnyId)
                    .closingPrice(closingPrice)
                    .highPrice(highPrice)
                    .lowPrice(lowPrice)
                    .buyQuantity(buyQuantity)
                    .sellQuantity(sellQuantity)
                    .tradeQuantity(tradeQuantity)
                    .marketCap(marketCap)
                    .build();

            bunnyHistoryRepository.save(history);
            bunnyIndicatorService.updateBunnyGrowth(bunny);
            bunnyIndicatorService.updateBunnyStability(bunny);
            bunnyIndicatorService.updateBunnyPopularity(bunny);
            bunnyIndicatorService.updateBunnyValue(bunny);
            bunnyIndicatorService.updateBunnyBalance(bunny);
        }
    }

    public PressureResponse getTop5ByPressure() {
        log.info("getTop5 by pressure");
        List<BunnyHistory> latest = bunnyHistoryRepository.findLatestPerCoin();

        final List<Bunny> bunnies = bunnyRepository.findAll();
        final Map<String, Bunny> bunnyMap = bunnies.stream()
                .collect(Collectors.toMap(Bunny::getId, Function.identity()));

        // 매수 강도 Top5 (buy ÷ sell × 100)
        List<BunnyPressureData> buyTop5 = latest.stream()
                .map(bh -> {
                    BigDecimal buy = bh.getBuyQuantity() != null ? bh.getBuyQuantity() : BigDecimal.ZERO;
                    BigDecimal sell = (bh.getSellQuantity() != null && bh.getSellQuantity().compareTo(BigDecimal.ZERO) > 0)
                            ? bh.getSellQuantity()
                            : BigDecimal.ONE;

                    BigDecimal pressure = buy.divide(sell, 4, RoundingMode.HALF_UP)
                                            .multiply(BigDecimal.valueOf(100));

                    Bunny bunny = bunnyMap.get(bh.getBunnyId());
                    String bunnyName = (bunny != null) ? bunny.getBunnyName() : "UNKNOWN";

                    return BunnyPressureData.of(bh.getBunnyId(), bunnyName, bh.getDate(), pressure);
                })
                .sorted((a, b) -> b.pressure().compareTo(a.pressure()))
                .limit(5)
                .toList();

        // 매도 강도 Top5 (sell ÷ buy × 100)
        List<BunnyPressureData> sellTop5 = latest.stream()
                .map(bh -> {
                    BigDecimal buy = (bh.getBuyQuantity() != null && bh.getBuyQuantity().compareTo(BigDecimal.ZERO) > 0)
                            ? bh.getBuyQuantity()
                            : BigDecimal.ONE; // 0 나눔 방지
                    BigDecimal sell = bh.getSellQuantity() != null ? bh.getSellQuantity() : BigDecimal.ZERO;

                    BigDecimal pressure = sell.divide(buy, 4, RoundingMode.HALF_UP)
                                            .multiply(BigDecimal.valueOf(100));

                    Bunny bunny = bunnyMap.get(bh.getBunnyId());
                    String bunnyName = (bunny != null) ? bunny.getBunnyName() : "UNKNOWN";

                    return BunnyPressureData.of(bh.getBunnyId(), bunnyName, bh.getDate(), pressure);
                })
                .sorted((a, b) -> b.pressure().compareTo(a.pressure()))
                .limit(5)
                .toList();

        return new PressureResponse(buyTop5, sellTop5);
    }
}
