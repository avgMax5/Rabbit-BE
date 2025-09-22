package team.avgmax.rabbit.bunny.repository.custom;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import com.querydsl.jpa.impl.JPAQuery;
import jakarta.persistence.LockModeType;
import org.springframework.stereotype.Repository;

import com.querydsl.core.types.Projections;
import com.querydsl.jpa.impl.JPAQueryFactory;

import lombok.RequiredArgsConstructor;

import team.avgmax.rabbit.bunny.dto.response.OrderListResponse;
import team.avgmax.rabbit.bunny.dto.response.OrderResponse;
import team.avgmax.rabbit.bunny.entity.Order;
import team.avgmax.rabbit.bunny.entity.QOrder;
import team.avgmax.rabbit.bunny.entity.enums.OrderType;

@Repository
@RequiredArgsConstructor
public class OrderRepositoryCustomImpl implements OrderRepositoryCustom {
    private final JPAQueryFactory queryFactory;

    @Override
    public OrderListResponse findOrdersByUserId(String personalUserId) {
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

        return OrderListResponse.builder()
                .orders(orders)
                .build();
    }

    @Override
    public List<Order> findSellCandidatesByPriceAsc(String bunnyId, BigDecimal buyPrice, String excludeUserId) {
        QOrder order = QOrder.order;

        return queryFactory
                .selectFrom(order)
                .where(
                        order.bunny.id.eq(bunnyId),
                        order.orderType.eq(OrderType.SELL),
                        order.unitPrice.loe(buyPrice),
                        order.user.id.ne(excludeUserId)
                )
                .orderBy(order.unitPrice.asc(), order.createdAt.asc())
                .fetch();
    }

    @Override
    public List<Order> findBuyCandidatesByPriceDesc(String bunnyId, BigDecimal sellPrice, String excludeUserId) {
        QOrder order = QOrder.order;

        return queryFactory
                .selectFrom(order)
                .where(
                        order.bunny.id.eq(bunnyId),
                        order.orderType.eq(OrderType.BUY),
                        order.unitPrice.goe(sellPrice),
                        order.user.id.ne(excludeUserId)
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

    @Override
    public List<Order> findAllByUserAndSideOrderByCreatedAtAsc(String userId, OrderType side) {
        QOrder order = QOrder.order;
        return queryFactory.selectFrom(order)
                .where(
                        order.user.id.eq(userId),
                        order.orderType.eq(side)
                )
                .orderBy(order.createdAt.asc())
                .fetch();
    }

    @Override
    public List<Order> findAllByUserAndBunnyAndSideOrderByCreatedAtAsc(String userId, String bunnyId, OrderType side) {
        QOrder order = QOrder.order;
        return queryFactory.selectFrom(order)
                .where(
                        order.user.id.eq(userId),
                        order.bunny.id.eq(bunnyId),
                        order.orderType.eq(side)
                )
                .orderBy(order.createdAt.asc())
                .fetch();
    }

    @Override
    public Order findByIdAndBunnyIdForUpdate(String orderId, String bunnyId) {
        QOrder order = QOrder.order;

        return queryFactory
                .selectFrom(order)
                .where(
                        order.id.eq(orderId),
                        order.bunny.id.eq(bunnyId)
                )
                .setLockMode(LockModeType.PESSIMISTIC_WRITE)
                .fetchFirst();
    }

    @Override
    public List<Order> findAllByBunnyAndSideForOrderBook(String bunnyId, OrderType side) {
        QOrder order = QOrder.order;
        int maxRows = 50;

        JPAQuery<Order> query = queryFactory
                .selectFrom(order)
                .where(
                        order.bunny.id.eq(bunnyId),
                        order.orderType.eq(side)
                );

        if (side == OrderType.BUY) {
            query.orderBy(order.unitPrice.desc(), order.createdAt.asc(), order.id.asc());
        } else {
            query.orderBy(order.unitPrice.asc(), order.createdAt.asc(), order.id.asc());
        }

        return query.limit(maxRows)
                .fetch();
    }

    @Override
    public List<Order> findAllByBunnySideAndPriceIn(String bunnyId, OrderType side, Set<BigDecimal> prices) {
        QOrder order = QOrder.order;

        return queryFactory
                .selectFrom(order)
                .where(
                        order.bunny.id.eq(bunnyId),
                        order.orderType.eq(side),
                        order.unitPrice.in(prices)
                )
                .fetch();
    }

}
