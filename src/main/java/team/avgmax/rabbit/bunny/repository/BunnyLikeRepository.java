package team.avgmax.rabbit.bunny.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import team.avgmax.rabbit.bunny.entity.BunnyLike;
import team.avgmax.rabbit.bunny.entity.id.BunnyLikeId;

public interface BunnyLikeRepository extends JpaRepository<BunnyLike, BunnyLikeId> {
    boolean existsByBunnyIdAndUserId(String bunnyId, String userId);
    void deleteByBunnyIdAndUserId(String bunnyId, String userId);
    List<BunnyLike> findByBunnyId(String bunnyId);
}
