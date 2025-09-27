package team.avgmax.rabbit.bunny.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import team.avgmax.rabbit.bunny.entity.BunnyHistory;
import team.avgmax.rabbit.bunny.entity.id.BunnyHistoryId;
import team.avgmax.rabbit.bunny.repository.custom.BunnyHistoryRepositoryCustom;

import java.util.List;

public interface BunnyHistoryRepository extends JpaRepository<BunnyHistory, BunnyHistoryId>, BunnyHistoryRepositoryCustom {

    List<BunnyHistory> findAllByBunnyIdOrderByDateAsc(String bunnyId);

    @Query("""
        SELECT bh
        FROM BunnyHistory bh
        WHERE bh.date = (
            SELECT MAX(bh2.date)
            FROM BunnyHistory bh2
            WHERE bh2.bunnyId = bh.bunnyId
        )
    """)
    List<BunnyHistory> findLatestPerCoin();
}