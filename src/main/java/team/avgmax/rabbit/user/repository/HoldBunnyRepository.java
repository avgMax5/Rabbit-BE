package team.avgmax.rabbit.user.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import team.avgmax.rabbit.user.entity.HoldBunny;
import team.avgmax.rabbit.user.repository.custom.HoldBunnyRepositoryCustom;

public interface HoldBunnyRepository extends JpaRepository<HoldBunny, String>, HoldBunnyRepositoryCustom {
}
