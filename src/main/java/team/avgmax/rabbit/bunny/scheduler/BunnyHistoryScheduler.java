package team.avgmax.rabbit.bunny.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import team.avgmax.rabbit.bunny.service.BunnyHistoryService;

import java.time.LocalDate;
import java.time.ZoneId;

@Slf4j
@Component
@RequiredArgsConstructor
public class BunnyHistoryScheduler {

    private final BunnyHistoryService bunnyHistoryService;

    @Scheduled(cron = "0 1 0 * * *", zone = "Asia/Seoul")
    public void recordBunnyHistory() {

        final ZoneId KST = ZoneId.of("Asia/Seoul");
        final LocalDate today = LocalDate.now(KST);
        final LocalDate targetDate = today.minusDays(1);

        log.info("BunnyHistoryScheduler: aggregate date={} (KST)", targetDate);
        bunnyHistoryService.aggregateFor(targetDate);
    }
}
