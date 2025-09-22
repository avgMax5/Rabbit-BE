package team.avgmax.rabbit.bunny.repository.custom;

import team.avgmax.rabbit.bunny.dto.response.OrderListResponse;
import team.avgmax.rabbit.bunny.entity.Order;
import team.avgmax.rabbit.bunny.entity.enums.OrderType;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

public interface OrderRepositoryCustom {
    OrderListResponse findOrdersByUserId(String personalUserId);

    List<Order> findSellCandidatesByPriceAsc(String bunnyId, BigDecimal buyPrice, String excludeUserId);

    List<Order> findBuyCandidatesByPriceDesc(String bunnyId, BigDecimal sellPrice, String excludeUserId);

    List<Order> findAllByUserAndSideOrderByCreatedAtAsc(String userId, OrderType side);

    List<Order> findAllByUserAndBunnyAndSideOrderByCreatedAtAsc(String userId, String bunnyId, OrderType side);

    Order findByIdAndBunnyIdForUpdate(String orderId, String bunnyId);

    List<Order> findAllByBunnyAndSideForOrderBook(String bunnyId, OrderType side);

    List<Order> findAllByBunnySideAndPriceIn(String bunnyId, OrderType side, Set<BigDecimal> prices);

    List<Order> lockedSellCandidatesByPriceAsc(String bunnyId, BigDecimal buyPrice, String excludeUserId);

    List<Order> lockedBuyCandidatesByPriceDesc(String bunnyId, BigDecimal sellPrice, String excludeUserId);
}