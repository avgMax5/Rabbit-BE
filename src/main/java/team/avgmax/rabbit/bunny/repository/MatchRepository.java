package team.avgmax.rabbit.bunny.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import team.avgmax.rabbit.bunny.entity.Match;
import team.avgmax.rabbit.bunny.repository.custom.MatchRepositoryCustom;

public interface MatchRepository extends JpaRepository<Match, String>, MatchRepositoryCustom {
}
