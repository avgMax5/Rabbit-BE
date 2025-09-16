package team.avgmax.rabbit.user.repository.custom;

import java.math.BigDecimal;
import java.util.List;

import com.querydsl.core.Tuple;
import org.springframework.stereotype.Repository;

import com.querydsl.core.types.Projections;
import com.querydsl.jpa.impl.JPAQueryFactory;

import lombok.RequiredArgsConstructor;
import team.avgmax.rabbit.bunny.dto.data.MyBunnyByHolderData;
import team.avgmax.rabbit.bunny.entity.QBunny;
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
                        holdBunny.totalBuyAmount
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
    public void addHoldForUpdate(String userId, String bunnyId, BigDecimal deltaQty) {
        QHoldBunny hold = QHoldBunny.holdBunny;

        long updated = queryFactory.update(hold)
                .set(hold.holdQuantity, hold.holdQuantity.add(deltaQty))
                .where(
                        hold.holder.id.eq(userId),
                        hold.bunny.id.eq(bunnyId)
                )
                .execute();

        if (updated == 0 && deltaQty.signum() > 0) {
            queryFactory.insert(hold)
                    .columns(hold.holder.id, hold.bunny.id, hold.holdQuantity, hold.totalBuyAmount)
                    .values(userId, bunnyId, deltaQty, BigDecimal.ZERO) // totalBuyAmount는 상황에 맞게
                    .execute();
        }
    }

}
