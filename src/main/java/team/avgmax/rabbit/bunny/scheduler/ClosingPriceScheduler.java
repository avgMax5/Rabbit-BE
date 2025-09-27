package team.avgmax.rabbit.bunny.scheduler;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import team.avgmax.rabbit.bunny.service.ClosingPriceService;

@Component
@RequiredArgsConstructor
public class ClosingPriceScheduler {

    private final ClosingPriceService closingPriceService;

    @Scheduled(cron = "0 1 0 * * *", zone = "Asia/Seoul")
    public void rollDailyClose() {
        // 내부에서 @Transactional + publishAfterCommit 사용
        closingPriceService.rollAndBroadcastDailyClose();
    }
}
