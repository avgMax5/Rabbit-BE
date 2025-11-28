package team.avgmax.rabbit.bunny.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import team.avgmax.rabbit.bunny.entity.Bunny;
import team.avgmax.rabbit.bunny.entity.BunnyLike;
import team.avgmax.rabbit.bunny.repository.BunnyLikeRepository;
import team.avgmax.rabbit.bunny.repository.BunnyRepository;
import team.avgmax.rabbit.bunny.service.BunnyIndicatorService;
import team.avgmax.rabbit.global.util.RedisUtil;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class BunnyLikeSyncScheduler {

    private final RedisUtil redisUtil;
    private final BunnyRepository bunnyRepository;
    private final BunnyLikeRepository bunnyLikeRepository;
    private final BunnyIndicatorService bunnyIndicatorService;

    private static final String LIKE_SET_KEY_PREFIX = "bunny_like:";
    private static final int CHUNK_SIZE = 50; // 한 번에 처리할 Bunny 수

    /**
     * 30분마다 Redis의 좋아요 데이터를 DB에 동기화
     */
    @Scheduled(fixedRate = 1800000)
    public void syncLikesToDatabase() {
        log.info("좋아요 동기화 스케줄러 시작");

        try {
            int page = 0;
            Page<Bunny> bunnyPage;

            // 페이징을 통해 청크 단위로 처리
            do {
                Pageable pageable = PageRequest.of(page, CHUNK_SIZE);
                bunnyPage = bunnyRepository.findAll(pageable);

                for (Bunny bunny : bunnyPage.getContent()) {
                    syncBunnyLikes(bunny);
                }

                page++;
                log.info("좋아요 동기화 진행: {}/{} 페이지 처리 완료", page, bunnyPage.getTotalPages());
            } while (bunnyPage.hasNext());

            log.info("좋아요 동기화 스케줄러 완료");
        } catch (Exception e) {
            log.error("좋아요 동기화 중 오류 발생", e);
        }
    }

    /**
     * 개별 Bunny의 좋아요를 동기화 (트랜잭션 분리)
     */
    @Transactional
    public void syncBunnyLikes(Bunny bunny) {
        try {
            String bunnyId = bunny.getId();
            String likeSetKey = LIKE_SET_KEY_PREFIX + bunnyId;

            // Redis Set에서 현재 좋아요한 사용자 목록 조회
            Set<Object> redisLikedUsers = redisUtil.getSetMembers(likeSetKey);
            
            if (redisLikedUsers == null || redisLikedUsers.isEmpty()) {
                return;
            }

            // DB에서 현재 좋아요한 사용자 목록 조회
            List<BunnyLike> dbLikedUsers = bunnyLikeRepository.findByBunnyId(bunnyId);
            Set<String> dbLikedUserIds = dbLikedUsers.stream()
                    .map(BunnyLike::getUserId)
                    .collect(Collectors.toSet());

            int likeCountDelta = 0;

            // Redis에 있지만 DB에 없는 사용자 → DB에 추가
            for (Object userIdObj : redisLikedUsers) {
                String userId = userIdObj.toString();
                if (!dbLikedUserIds.contains(userId)) {
                    bunnyLikeRepository.save(BunnyLike.create(bunnyId, userId));
                    likeCountDelta++;
                }
            }

            // DB에 있지만 Redis에 없는 사용자 → DB에서 삭제 (좋아요 취소된 경우)
            for (BunnyLike dbLike : dbLikedUsers) {
                String userId = dbLike.getUserId();
                boolean existsInRedis = redisLikedUsers.stream()
                        .anyMatch(obj -> obj.toString().equals(userId));
                if (!existsInRedis) {
                    bunnyLikeRepository.deleteByBunnyIdAndUserId(bunnyId, userId);
                    likeCountDelta--;
                }
            }

            // Bunny의 likeCount 업데이트
            if (likeCountDelta != 0) {
                if (likeCountDelta > 0) {
                    for (int i = 0; i < likeCountDelta; i++) {
                        bunny.addLikeCount();
                    }
                } else {
                    for (int i = 0; i < Math.abs(likeCountDelta); i++) {
                        bunny.subtractLikeCount();
                    }
                }

                // 지표 업데이트
                bunnyIndicatorService.updateBunnyReliability(bunny);
                bunnyIndicatorService.updateBunnyValue(bunny);
                bunnyIndicatorService.updateBunnyPopularity(bunny);
            }

            log.debug("Bunny {} 좋아요 동기화 완료: delta={}", bunny.getBunnyName(), likeCountDelta);
        } catch (Exception e) {
            log.error("Bunny {} 좋아요 동기화 중 오류 발생", bunny.getBunnyName(), e);
            // 개별 Bunny 처리 실패는 로깅만 하고 계속 진행
        }
    }
}
