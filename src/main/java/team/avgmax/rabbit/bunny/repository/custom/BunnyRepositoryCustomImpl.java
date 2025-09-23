package team.avgmax.rabbit.bunny.repository.custom;

import com.querydsl.core.types.dsl.CaseBuilder;
import com.querydsl.core.types.dsl.NumberExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import team.avgmax.rabbit.bunny.entity.Bunny;
import team.avgmax.rabbit.bunny.entity.enums.BunnyType;
import team.avgmax.rabbit.bunny.entity.enums.DeveloperType;
import team.avgmax.rabbit.user.entity.enums.Position;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

import static team.avgmax.rabbit.bunny.entity.QBadge.badge;
import static team.avgmax.rabbit.bunny.entity.QBunny.bunny;
import static team.avgmax.rabbit.user.entity.QPersonalUser.personalUser;

@RequiredArgsConstructor
public class BunnyRepositoryCustomImpl implements BunnyRepositoryCustom{

    private final JPAQueryFactory queryFactory;
    private static final long BADGE_COUNT = 3L;

    @Override
    public List<Bunny> findAllByPriorityGroupAndCreatedAt() {
        NumberExpression<Integer> priorityGroup = new CaseBuilder()
                .when(badge.count().eq(BADGE_COUNT)).then(0).otherwise(1);

        return queryFactory
                .select(bunny)
                .from(bunny)
                .leftJoin(badge).on(bunny.id.eq(badge.bunnyId))
                .groupBy((bunny.id))
                .orderBy(
                        priorityGroup.asc(),
                        bunny.createdAt.asc()
                )
                .fetch();
    }

    @Override
    public BigDecimal findAverageGrowthRateByBunnyType(BunnyType bunnyType) {
        return calculateAverageGrowthRate(bunny.bunnyType.eq(bunnyType));
    }

    @Override
    public BigDecimal findAverageGrowthRateByPosition(Position position) {
        return calculateAverageGrowthRate(bunny.user.position.eq(position));
    }

    @Override
    public BigDecimal findAverageGrowthRateByDeveloperType(DeveloperType developerType) {
        return calculateAverageGrowthRate(bunny.developerType.eq(developerType));
    }

    @Override
    public List<Bunny> findAllWithUserOrderByMarketCapDesc() {
        return queryFactory
                .selectFrom(bunny)
                .join(bunny.user, personalUser).fetchJoin()
                .orderBy(bunny.marketCap.desc())
                .fetch();
    }

    @Override
    public BigDecimal sumCurrentMarketCap() {
        BigDecimal sum = queryFactory
                .select(bunny.marketCap.sum())
                .from(bunny)
                .where(bunny.marketCap.isNotNull())
                .fetchOne();
        
        return sum != null ? sum : BigDecimal.ZERO;
    }

    private BigDecimal calculateAverageGrowthRate(com.querydsl.core.types.dsl.BooleanExpression condition) {
        NumberExpression<BigDecimal> growthRate = bunny.currentPrice.subtract(bunny.closingPrice)
                .divide(bunny.closingPrice)
                .multiply(100);

        Double averageDouble = queryFactory
                .select(growthRate.avg())
                .from(bunny)
                .where(
                        condition,
                        bunny.closingPrice.isNotNull(),
                        bunny.closingPrice.ne(BigDecimal.ZERO)
                )
                .fetchOne();

        BigDecimal average = (averageDouble != null) ? BigDecimal.valueOf(averageDouble) : null;

        return (average != null) ? average.setScale(4, RoundingMode.HALF_UP) : BigDecimal.ZERO;
    }

}
