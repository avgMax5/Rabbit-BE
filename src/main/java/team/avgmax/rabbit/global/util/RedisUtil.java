package team.avgmax.rabbit.global.util;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class RedisUtil {

    private final RedisTemplate<String, Object> redisTemplate;

    public void setData(String key, String value, Long expiredTime){
        redisTemplate.opsForValue().set(key, value, expiredTime, TimeUnit.MILLISECONDS);
    }

    public void setData(String key, List<String> value, Long expiredTime){
        redisTemplate.opsForList().rightPushAll(key, value);
        redisTemplate.expire(key, expiredTime, TimeUnit.MILLISECONDS);
    }

    public String getData(String key){
        return (String) redisTemplate.opsForValue().get(key);
    }

    public void deleteData(String key){
        redisTemplate.delete(key);
    }

    /**
     * FundBunny 만료 시간을 설정합니다.
     * @param fundBunnyId FundBunny ID
     * @param expirationTimeMillis 만료 시간 (밀리초)
     */
    public void setFundBunnyExpiration(String fundBunnyId, long expirationTimeMillis) {
        String key = "fund_bunny:" + fundBunnyId;
        redisTemplate.opsForValue().set(key, "expiration_marker", expirationTimeMillis, TimeUnit.MILLISECONDS);
    }

    /**
     * FundBunny 만료 시간을 제거합니다.
     * @param fundBunnyId FundBunny ID
     */
    public void removeFundBunnyExpiration(String fundBunnyId) {
        String key = "fund_bunny:" + fundBunnyId;
        redisTemplate.delete(key);
    }
}