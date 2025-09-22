package team.avgmax.rabbit.bunny.repository.custom;

import team.avgmax.rabbit.bunny.dto.response.ChartDataPoint;
import team.avgmax.rabbit.bunny.entity.BunnyHistory;
import team.avgmax.rabbit.bunny.entity.enums.ChartInterval;

import java.util.List;

public interface BunnyHistoryRepositoryCustom {

    List<ChartDataPoint> findChartData(String bunnyId, ChartInterval interval);
    
    List<BunnyHistory> findRecentByBunnyIdOrderByDateAsc(String bunnyId, int days);
}
