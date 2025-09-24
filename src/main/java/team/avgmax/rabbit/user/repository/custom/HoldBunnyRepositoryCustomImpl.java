package team.avgmax.rabbit.user.repository.custom;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import com.querydsl.core.Tuple;
import com.querydsl.core.types.dsl.CaseBuilder;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.core.types.dsl.NumberExpression;
import com.querydsl.jpa.JPAExpressions;
import org.springframework.stereotype.Repository;

import com.querydsl.core.types.Projections;
import com.querydsl.jpa.impl.JPAQueryFactory;

import lombok.RequiredArgsConstructor;

import team.avgmax.rabbit.bunny.dto.data.MyBunnyByHolderData;
import team.avgmax.rabbit.bunny.entity.QBunny;
import team.avgmax.rabbit.bunny.entity.QOrder;
import team.avgmax.rabbit.bunny.entity.enums.OrderType;
import team.avgmax.rabbit.bunny.exception.BunnyError;
import team.avgmax.rabbit.bunny.exception.BunnyException;
import team.avgmax.rabbit.global.util.UlidGenerator;
import team.avgmax.rabbit.user.entity.QHoldBunny;
import team.avgmax.rabbit.user.entity.QPersonalUser;

@Repository
@RequiredArgsConstructor
public class HoldBunnyRepositoryCustomImpl implements HoldBunnyRepositoryCustom {
    private final JPAQueryFactory queryFactory;

    @Override
    public List<MyBunnyByHolderData> findHoldersByBunnyId(String bunnyId) {
        QHoldBunny holdBunny = QHoldBunny.holdBunny;
        QPersonalUser holder = QPersonalUser.personalUser;

        return queryFactory
                .select(Projections.constructor(MyBunnyByHolderData.class,
                        holder.id,
                        holder.name,
                        holder.image,
                        holdBunny.holdQuantity
                ))
                .from(holdBunny)
                .join(holdBunny.holder, holder)
                .where(holdBunny.bunny.id.eq(bunnyId))
                .orderBy(holdBunny.holdQuantity.desc())
                .fetch();
    }

    @Override
    public List<Tuple> findHolderTypeDistributionByBunnyId(String bunnyId) {
        QHoldBunny holdBunny = QHoldBunny.holdBunny;
        QPersonalUser holder = QPersonalUser.personalUser;
        QBunny holderBunny = new QBunny("holderBunny");

        return queryFactory
                .select(
                        holderBunny.developerType,
                        holdBunny.holdQuantity.sum(),
                        holder.id.countDistinct()
                )
                .from(holdBunny)
                .join(holdBunny.holder, holder)
                .leftJoin(holderBunny).on(holderBunny.user.id.eq(holder.id))
                .where(holdBunny.bunny.id.eq(bunnyId))
                .groupBy(holderBunny.developerType)
                .fetch();
    }

    @Override
    public void applyBuyMatch(String userId, String bunnyId, BigDecimal qty, BigDecimal tradeBaseAmount) {
        QHoldBunny hold = QHoldBunny.holdBunny;

        long updated = queryFactory.update(hold)
                .set(hold.holdQuantity, hold.holdQuantity.add(qty))
                .set(hold.costBasis, hold.costBasis.add(tradeBaseAmount))
                .where(hold.holder.id.eq(userId), hold.bunny.id.eq(bunnyId))
                .execute();

        if (updated == 0) {
            String id = UlidGenerator.generateMonotonic();
            queryFactory.insert(hold)
                    .columns(hold.id, hold.holder.id, hold.bunny.id, hold.holdQuantity, hold.costBasis, hold.createdAt, hold.updatedAt)
                    .values(id, userId, bunnyId, qty, tradeBaseAmount, LocalDateTime.now(), LocalDateTime.now())
                    .execute();
        }
    }

    @Override
    public void applySellMatch(String userId, String bunnyId, BigDecimal filledQty) {
        if (filledQty == null || filledQty.signum() <= 0) return;
        QHoldBunny hold = QHoldBunny.holdBunny;

        // oldQty = 현재 holdQuantity + filledQty (체결 전 보유 수량)
        // newQty = holdQuantity (체결 후 보유 수량)
        NumberExpression<BigDecimal> oldQty = hold.holdQuantity.add(filledQty);

        // newCostBasis = costBasis * newQty / oldQty
        NumberExpression<BigDecimal> newCostExpr =
                new CaseBuilder()
                        .when(oldQty.gt(BigDecimal.ZERO))
                        .then(hold.costBasis.multiply(hold.holdQuantity).divide(oldQty))
                        .otherwise(Expressions.constant(BigDecimal.ZERO));

        queryFactory.update(hold)
                .set(hold.costBasis, newCostExpr)
                .where(
                        hold.holder.id.eq(userId),
                        hold.bunny.id.eq(bunnyId)
                )
                .execute();
    }

    @Override
    public void adjustReservation(String userId, String bunnyId, BigDecimal deltaQty) {
        QHoldBunny hold = QHoldBunny.holdBunny;

        long updated = queryFactory.update(hold)
                .set(hold.holdQuantity, hold.holdQuantity.add(deltaQty))
                .where(hold.holder.id.eq(userId), hold.bunny.id.eq(bunnyId))
                .execute();

        if (updated == 0 && deltaQty.signum() > 0) {
            String id = UlidGenerator.generateMonotonic();
            queryFactory.insert(hold)
                    .columns(hold.id, hold.holder.id, hold.bunny.id, hold.holdQuantity, hold.costBasis, hold.createdAt, hold.updatedAt)
                    .values(id, userId, bunnyId, deltaQty, BigDecimal.ZERO, LocalDateTime.now(), LocalDateTime.now())
                    .execute();
        }
    }

    @Override
    public void deleteIfEmpty(String userId, String bunnyId) {
        QHoldBunny hold = QHoldBunny.holdBunny;
        QOrder order = QOrder.order;

        // 현재 보유량 조회
        BigDecimal qty = queryFactory
                .select(hold.holdQuantity)
                .from(hold)
                .where(hold.holder.id.eq(userId), hold.bunny.id.eq(bunnyId))
                .fetchOne();

        // 행이 없으면 끝
        if (qty == null) return;

        // 음수면 즉시 예외 (트랜잭션 롤백)
        if (qty.signum() < 0) {
            throw new BunnyException(BunnyError.NEGATIVE_HOLDING);
        } else if (qty.signum() > 0) return;

        // 열린 SELL 주문 존재 여부
        var hasOpenSell = JPAExpressions
                .selectOne()
                .from(order)
                .where(
                        order.user.id.eq(userId),
                        order.bunny.id.eq(bunnyId),
                        order.orderType.eq(OrderType.SELL),
                        order.quantity.gt(BigDecimal.ZERO) // 잔량 > 0 인 열린 주문
                );

        // 보유가 0이고, 열린 SELL 주문이 없을 때만 삭제
        queryFactory.delete(hold)
                .where(
                        hold.holder.id.eq(userId),
                        hold.bunny.id.eq(bunnyId),
                        // 열린 SELL 주문이 "없을 때만" 삭제
                        hasOpenSell.notExists()
                )
                .execute();
    }
}
