package team.avgmax.rabbit.user.repository.custom;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import com.querydsl.core.Tuple;
import com.querydsl.core.types.dsl.NumberExpression;
import org.springframework.stereotype.Repository;

import com.querydsl.core.types.Projections;
import com.querydsl.jpa.impl.JPAQueryFactory;

import lombok.RequiredArgsConstructor;
import team.avgmax.rabbit.bunny.dto.data.MyBunnyByHolderData;
import team.avgmax.rabbit.bunny.entity.QBunny;
import team.avgmax.rabbit.global.util.UlidGenerator;
import team.avgmax.rabbit.user.dto.response.HoldBunniesResponse;
import team.avgmax.rabbit.user.dto.response.HoldBunnyResponse;
import team.avgmax.rabbit.user.entity.QHoldBunny;
import team.avgmax.rabbit.user.entity.QPersonalUser;

@Repository
@RequiredArgsConstructor
public class HoldBunnyRepositoryCustomImpl implements HoldBunnyRepositoryCustom {
    private final JPAQueryFactory queryFactory;

    @Override
    public HoldBunniesResponse findHoldBunniesByUserId(String personalUserId) {
        QHoldBunny holdBunny = QHoldBunny.holdBunny;

        List<HoldBunnyResponse> holdBunnies = queryFactory
                .select(Projections.constructor(HoldBunnyResponse.class,
                        holdBunny.bunny.id,
                        holdBunny.bunny.bunnyName,
                        holdBunny.holdQuantity,
                        holdBunny.costBasis
                        ))
                .from(holdBunny)
                .where(holdBunny.holder.id.eq(personalUserId))
                .fetch();

        return HoldBunniesResponse.builder()
                .holdBunnies(holdBunnies)
                .build();
    }

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
    public void upsertForTrade(String userId, String bunnyId, BigDecimal qtyDelta, BigDecimal tradeBaseAmount, boolean adjustCost) {

        QHoldBunny hold = QHoldBunny.holdBunny;
        NumberExpression<BigDecimal> newQtyExpr = hold.holdQuantity.add(qtyDelta);

        // costBasis 계산 로직
        NumberExpression<BigDecimal> newCostExpr =
                com.querydsl.core.types.dsl.Expressions.cases()
                        // 예약이면 원가 유지
                        .when(com.querydsl.core.types.dsl.Expressions.booleanTemplate("{0} = false", adjustCost))
                        .then(hold.costBasis)
                        // 매수 체결이면 원가 += 체결 원금
                        .when(com.querydsl.core.types.dsl.Expressions.booleanTemplate("{0} > 0", qtyDelta))
                        .then(hold.costBasis.add(tradeBaseAmount))
                        // 매도 체결이면 원가 비율 축소
                        .when(com.querydsl.core.types.dsl.Expressions.booleanTemplate("{0} < 0 AND {1} > 0", qtyDelta, hold.holdQuantity))
                        .then(
                                hold.costBasis
                                        .multiply(newQtyExpr)
                                        .divide(hold.holdQuantity) // 정수 원단위로 관리, 필요시 반올림 조정
                        )
                        .otherwise(hold.costBasis);

        long updated = queryFactory.update(hold)
                .set(hold.holdQuantity, newQtyExpr)
                .set(hold.costBasis, newCostExpr)
                .where(hold.holder.id.eq(userId), hold.bunny.id.eq(bunnyId))
                .execute();

        // 기존 보유가 없는데 매수 체결이라면 새 insert
        if (updated == 0 && qtyDelta.signum() > 0 && adjustCost) {
            String id = UlidGenerator.generateMonotonic();
            queryFactory.insert(hold)
                    .columns(hold.id, hold.holder.id, hold.bunny.id, hold.holdQuantity, hold.costBasis, hold.createdAt, hold.updatedAt)
                    .values(id, userId, bunnyId, qtyDelta, tradeBaseAmount, LocalDateTime.now(), LocalDateTime.now())
                    .execute();
            return;
        }

        // 삭제는 체결(adjustCost=true)일 때만 허용
        if (adjustCost) {
            queryFactory.delete(hold)
                    .where(
                            hold.holder.id.eq(userId),
                            hold.bunny.id.eq(bunnyId),
                            hold.holdQuantity.loe(BigDecimal.ZERO)
                    )
                    .execute();
        }
    }

    @Override
    public void addHoldForUpdate(String userId, String bunnyId, BigDecimal deltaQty) {
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
}
