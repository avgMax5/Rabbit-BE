package team.avgmax.rabbit.bunny.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import team.avgmax.rabbit.bunny.controller.currentPrice.PriceTickPublisher;
import team.avgmax.rabbit.bunny.dto.currentPrice.ClosingPriceUpdate;
import team.avgmax.rabbit.bunny.entity.Bunny;
import team.avgmax.rabbit.bunny.entity.BunnyHistory;
import team.avgmax.rabbit.bunny.repository.BunnyHistoryRepository;
import team.avgmax.rabbit.bunny.repository.BunnyRepository;
import team.avgmax.rabbit.bunny.repository.MatchRepository;

import java.math.BigDecimal;
import java.time.ZoneId;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ClosingPriceService {

    private final BunnyRepository bunnyRepository;
    private final BunnyHistoryRepository bunnyHistoryRepository;
    private final PriceTickPublisher priceTickPublisher;
    private final MatchRepository matchRepository;

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    @Transactional
    public void rollAndBroadcastDailyClose() {
        // 00:01(KST)에 호출 됨
        final var targetDate = java.time.LocalDate.now(KST).minusDays(1);

        List<Bunny> all = bunnyRepository.findAll();
        for (Bunny bunny : all) {
            BigDecimal closingPrice = computeCloseFor(bunny);
            bunny.updateClosingPrice(closingPrice);

            // 전일자 히스토리 적재
            bunnyHistoryRepository.save(BunnyHistory.of(bunny.getId(), targetDate, closingPrice));
        }

        // 커밋 후에만 WS 발행 (전일 종가 broadcast 1회)
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override public void afterCommit() {
                for (Bunny bunny : all) {
                    priceTickPublisher.publishClose(new ClosingPriceUpdate(
                            bunny.getBunnyName(),
                            bunny.getClosingPrice(),
                            targetDate
                    ));
                }
            }
        });
    }

    private BigDecimal computeCloseFor(Bunny bunny) {
        // 마지막 체결가 → 현재가 → 직전 종가 → 0 순서로 폴백
        BigDecimal lastTrade = matchRepository.findLastTradePriceByBunnyId(bunny.getId());
        if (lastTrade != null) return lastTrade;
        if (bunny.getCurrentPrice() != null) return bunny.getCurrentPrice();
        if (bunny.getClosingPrice() != null) return bunny.getClosingPrice();
        return BigDecimal.ZERO;
    }
}
