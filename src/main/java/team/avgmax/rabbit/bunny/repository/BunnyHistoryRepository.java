package team.avgmax.rabbit.bunny.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import team.avgmax.rabbit.bunny.entity.BunnyHistory;

import java.util.List;

public interface BunnyHistoryRepository extends JpaRepository<BunnyHistory, String> {

    List<BunnyHistory> findAllByBunnyIdOrderByDateAsc(String bunnyId);
}