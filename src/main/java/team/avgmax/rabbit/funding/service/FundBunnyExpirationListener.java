package team.avgmax.rabbit.funding.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.data.redis.listener.PatternTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;

@Slf4j
@Component
@RequiredArgsConstructor
public class FundBunnyExpirationListener implements MessageListener {

    private final RedisMessageListenerContainer redisMessageListenerContainer;
    private final FundingService fundingService;

    @PostConstruct
    public void init() {
        // Redis 키 만료 이벤트를 구독합니다
        redisMessageListenerContainer.addMessageListener(this, new PatternTopic("__keyevent@0__:expired"));
        log.info("FundBunny 만료 이벤트 리스너가 시작되었습니다.");
    }

    @Override
    public void onMessage(@NonNull Message message, @Nullable byte[] pattern) {
        String expiredKey = new String(message.getBody());
        log.info("Redis 키가 만료되었습니다: {}", expiredKey);

        // FundBunny 관련 키인지 확인
        if (expiredKey.startsWith("fund_bunny:")) {
            String fundBunnyId = expiredKey.replace("fund_bunny:", "");
            log.info("FundBunny 만료 이벤트 감지: {}", fundBunnyId);
            
            try {
                fundingService.processFundBunnyExpiration(fundBunnyId);
                log.info("FundBunny 만료 처리 완료: {}", fundBunnyId);
            } catch (Exception e) {
                log.error("FundBunny 만료 처리 중 오류 발생: {}", fundBunnyId, e);
            }
        }
    }
}
