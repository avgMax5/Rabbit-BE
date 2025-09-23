package team.avgmax.rabbit.bunny.repository.custom;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

import com.querydsl.jpa.impl.JPAQuery;
import jakarta.persistence.LockModeType;
import org.springframework.stereotype.Repository;

import com.querydsl.jpa.impl.JPAQueryFactory;

import lombok.RequiredArgsConstructor;

import team.avgmax.rabbit.bunny.entity.Order;
import team.avgmax.rabbit.bunny.entity.QOrder;
import team.avgmax.rabbit.bunny.entity.enums.OrderType;

@Repository
@RequiredArgsConstructor
public class OrderRepositoryCustomImpl implements OrderRepositoryCustom {
    private final JPAQueryFactory queryFactory;

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
                        order.orderType.eq(side),
                        order.quantity.gt(BigDecimal.ZERO)
                );

        if (side == OrderType.BUY) {
            query.orderBy(order.unitPrice.desc(), order.createdAt.asc(), order.id.asc());
        } else {
            query.orderBy(order.unitPrice.asc(), order.createdAt.asc(), order.id.asc());
        }

        return query.limit(maxRows).fetch();
    }

    @Override
    public List<Order> findAllByBunnySideAndPriceIn(String bunnyId, OrderType side, Set<BigDecimal> prices) {
        QOrder order = QOrder.order;

        return queryFactory
                .selectFrom(order)
                .where(
                        order.bunny.id.eq(bunnyId),
                        order.orderType.eq(side),
                        order.unitPrice.in(prices),
                        order.quantity.gt(BigDecimal.ZERO)
                )
                .fetch();
    }

    @Override
    public List<Order> lockedSellCandidatesByPriceAsc(String bunnyId, BigDecimal buyPrice, String excludeUserId) {
        QOrder order = QOrder.order;
        return queryFactory.selectFrom(order)
                .where(
                        order.bunny.id.eq(bunnyId),
                        order.orderType.eq(OrderType.SELL),
                        order.unitPrice.loe(buyPrice),
                        order.user.id.ne(excludeUserId),
                        order.quantity.gt(BigDecimal.ZERO)
                )
                .orderBy(order.unitPrice.asc(), order.createdAt.asc(), order.id.asc())
                .setLockMode(LockModeType.PESSIMISTIC_WRITE)
                .fetch();
    }

    @Override
    public List<Order> lockedBuyCandidatesByPriceDesc(String bunnyId, BigDecimal sellPrice, String excludeUserId) {
        QOrder order = QOrder.order;
        return queryFactory.selectFrom(order)
                .where(
                        order.bunny.id.eq(bunnyId),
                        order.orderType.eq(OrderType.BUY),
                        order.unitPrice.goe(sellPrice),
                        order.user.id.ne(excludeUserId),
                        order.quantity.gt(BigDecimal.ZERO)
                )
                .orderBy(order.unitPrice.desc(), order.createdAt.asc(), order.id.asc())
                .setLockMode(LockModeType.PESSIMISTIC_WRITE)
                .fetch();
    }

}
