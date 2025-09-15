package team.avgmax.rabbit.user.repository.custom;

import team.avgmax.rabbit.bunny.entity.Order;
import team.avgmax.rabbit.bunny.entity.enums.OrderType;
import team.avgmax.rabbit.user.dto.response.OrdersResponse;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public interface OrderRepositoryCustom {
    OrdersResponse findOrdersByUserId(String personalUserId);

    List<Order> findSellCandidatesByPriceAsc(String bunnyId, BigDecimal buyPrice);

    List<Order> findBuyCandidatesByPriceDesc(String bunnyId, BigDecimal sellPrice);

    BigDecimal sumPrevOrdersQuantity(String bunnyId, String userId, OrderType side, BigDecimal price, LocalDateTime currentCreatedAt);
}