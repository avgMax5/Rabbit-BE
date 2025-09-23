package team.avgmax.rabbit.bunny.repository.custom;

import team.avgmax.rabbit.bunny.entity.Match;
import team.avgmax.rabbit.bunny.entity.enums.OrderType;

import java.math.BigDecimal;
import java.util.List;

public interface MatchRepositoryCustom {
    List<Match> findMatchesByUserId(String userId);

    BigDecimal findLastTradePriceByBunnyId(String bunnyId);
}
