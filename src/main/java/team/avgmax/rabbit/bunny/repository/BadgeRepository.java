package team.avgmax.rabbit.bunny.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import team.avgmax.rabbit.bunny.entity.Badge;
import team.avgmax.rabbit.bunny.entity.id.BadgeId;

import java.util.List;

public interface BadgeRepository extends JpaRepository<Badge, BadgeId> {
    void deleteByBunnyIdAndUserId(String bunnyId, String userId);
}
