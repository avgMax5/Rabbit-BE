package team.avgmax.rabbit.bunny.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import team.avgmax.rabbit.bunny.entity.BunnyHistory;
import team.avgmax.rabbit.bunny.repository.custom.BunnyHistoryRepositoryCustom;

import java.util.List;

public interface BunnyHistoryRepository extends JpaRepository<BunnyHistory, String>, BunnyHistoryRepositoryCustom {

    List<BunnyHistory> findAllByBunnyIdOrderByDateAsc(String bunnyId);
}