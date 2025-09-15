package team.avgmax.rabbit.user.repository.custom;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Repository;

import com.querydsl.core.types.Projections;
import com.querydsl.jpa.impl.JPAQueryFactory;

import lombok.RequiredArgsConstructor;

import team.avgmax.rabbit.bunny.entity.Order;
import team.avgmax.rabbit.bunny.entity.QOrder;
import team.avgmax.rabbit.bunny.entity.enums.OrderType;
import team.avgmax.rabbit.user.dto.response.OrderResponse;
import team.avgmax.rabbit.user.dto.response.OrdersResponse;

@Repository
@RequiredArgsConstructor
public class OrderRepositoryCustomImpl implements OrderRepositoryCustom {
    private final JPAQueryFactory queryFactory;

    @Override
    public OrdersResponse findOrdersByUserId(String personalUserId) {
        QOrder order = QOrder.order;

        List<OrderResponse> orders = queryFactory
                .select(Projections.constructor(OrderResponse.class,
                        order.id,
                        order.bunny.bunnyName,
                        order.bunny.id,
                        order.quantity,
                        order.unitPrice,
                        order.orderType.stringValue()))
                .from(order)
                .where(order.user.id.eq(personalUserId))
                .fetch();

        return OrdersResponse.builder()
                .orders(orders)
                .build();
    }

    @Override
    public List<Order> findSellCandidatesByPriceAsc(String bunnyId, BigDecimal buyPrice) {
        QOrder order = QOrder.order;

        return queryFactory
                .selectFrom(order)
                .where(
                        order.bunny.id.eq(bunnyId),
                        order.orderType.eq(OrderType.SELL),
                        order.unitPrice.loe(buyPrice)
                )
                .orderBy(order.unitPrice.asc(), order.createdAt.asc())
                .fetch();
    }

    @Override
    public List<Order> findBuyCandidatesByPriceDesc(String bunnyId, BigDecimal sellPrice) {
        QOrder order = QOrder.order;

        return queryFactory
                .selectFrom(order)
                .where(
                        order.bunny.id.eq(bunnyId),
                        order.orderType.eq(OrderType.BUY),
                        order.unitPrice.goe(sellPrice)
                )
                .orderBy(order.unitPrice.desc(), order.createdAt.asc())
                .fetch();
    }

    @Override
    public BigDecimal sumPrevOrdersQuantity(String bunnyId, String userId, OrderType side, BigDecimal price, LocalDateTime currentCreatedAt) {
        QOrder order = QOrder.order;

        BigDecimal sum = queryFactory.select(order.quantity.sum())
                .from(order)
                .where(
                        order.bunny.id.eq(bunnyId),
                        order.user.id.eq(userId),
                        order.orderType.eq(side),
                        order.unitPrice.eq(price),
                        order.createdAt.before(currentCreatedAt)
                )
                .fetchOne();

        return sum != null ? sum : BigDecimal.ZERO;
    }

}
