package team.avgmax.rabbit.bunny.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;

@Slf4j
@Component
@RequiredArgsConstructor
public class BunnyHistorySchedulerDryRun {

    // 매일 KST 00:05 실행 (운영 안정성 고려해 약간 딜레이)
    @Scheduled(cron = "0 5 0 * * *", zone = "Asia/Seoul")
    public void dryRun() {
        ZoneId KST = ZoneId.of("Asia/Seoul");
        LocalDate today  = LocalDate.now(KST);
        LocalDate target = today.minusDays(1);

        LocalDateTime from = target.atStartOfDay(); // 어제 00:00
        LocalDateTime to   = today.atStartOfDay();  // 오늘 00:00

        log.info("[BunnyHistorySchedulerDryRun] window={} ~ {}", from, to);
        // 다음 단계에서: 이 윈도우에 대해 Match 집계 → BunnyHistory upsert
    }
}
