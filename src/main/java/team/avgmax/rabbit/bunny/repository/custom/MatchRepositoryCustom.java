package team.avgmax.rabbit.bunny.repository.custom;

import team.avgmax.rabbit.bunny.entity.enums.OrderType;

import java.math.BigDecimal;

public interface MatchRepositoryCustom {

    BigDecimal sumFilledByUserSideAndPrice(String bunnyId, String userId, OrderType side, BigDecimal price);
}
