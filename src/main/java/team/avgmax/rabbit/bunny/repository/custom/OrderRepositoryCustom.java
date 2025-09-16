package team.avgmax.rabbit.bunny.repository.custom;

import team.avgmax.rabbit.bunny.dto.response.OrderListResponse;
import team.avgmax.rabbit.bunny.entity.Order;
import team.avgmax.rabbit.bunny.entity.enums.OrderType;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public interface OrderRepositoryCustom {
    OrderListResponse findOrdersByUserId(String personalUserId);

    List<Order> findSellCandidatesByPriceAsc(String bunnyId, BigDecimal buyPrice, String excludeUserId);

    List<Order> findBuyCandidatesByPriceDesc(String bunnyId, BigDecimal sellPrice, String excludeUserId);

    BigDecimal sumPrevOrdersQuantity(String bunnyId, String userId, OrderType side, BigDecimal price, LocalDateTime currentCreatedAt);

    List<Order> findAllByUserAndSideOrderByCreatedAtAsc(String userId, OrderType side);

    List<Order> findAllByUserAndBunnyAndSideOrderByCreatedAtAsc(String userId, String bunnyId, OrderType side);
}