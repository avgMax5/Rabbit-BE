package team.avgmax.rabbit.user.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import team.avgmax.rabbit.user.entity.CorporationUser;

public interface CorporationUserRepository extends JpaRepository<CorporationUser, String> {
}
