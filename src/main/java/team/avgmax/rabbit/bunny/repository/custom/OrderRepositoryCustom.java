package team.avgmax.rabbit.bunny.repository.custom;

import team.avgmax.rabbit.bunny.entity.Order;
import team.avgmax.rabbit.bunny.entity.enums.OrderType;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

public interface OrderRepositoryCustom {
    Order findByIdAndBunnyIdForUpdate(String orderId, String bunnyId);

    List<Order> findAllByBunnyAndSideForOrderBook(String bunnyId, OrderType side);

    List<Order> findAllByBunnySideAndPriceIn(String bunnyId, OrderType side, Set<BigDecimal> prices);

    List<Order> lockedSellCandidatesByPriceAsc(String bunnyId, BigDecimal buyPrice, String excludeUserId);

    List<Order> lockedBuyCandidatesByPriceDesc(String bunnyId, BigDecimal sellPrice, String excludeUserId);
}