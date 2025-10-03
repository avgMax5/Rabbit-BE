package team.avgmax.rabbit.bunny.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

import team.avgmax.rabbit.bunny.entity.Badge;
import team.avgmax.rabbit.bunny.entity.id.BadgeId;

public interface BadgeRepository extends JpaRepository<Badge, BadgeId> {
    void deleteByBunnyIdAndUserId(String bunnyId, String userId);
    List<Badge> findAllByBadgeImg(String badgeImg);
}
