package team.avgmax.rabbit.bunny.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import team.avgmax.rabbit.bunny.entity.Badge;
import team.avgmax.rabbit.bunny.entity.id.BadgeId;

import java.util.List;

public interface BadgeRepository extends JpaRepository<Badge, BadgeId> {

    List<Badge> findAllByBunnyIdIn(List<String> bunnyIds);

    List<Badge> findAllByBunnyId(String bunnyId);

    void deleteByBunnyIdAndUserId(String bunnyId, String userId);
}
