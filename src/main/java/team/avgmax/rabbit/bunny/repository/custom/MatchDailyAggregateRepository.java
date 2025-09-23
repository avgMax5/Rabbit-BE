package team.avgmax.rabbit.bunny.repository.custom;

import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import team.avgmax.rabbit.bunny.entity.QMatch;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class MatchDailyAggregateRepository {

    private final JPAQueryFactory queryFactory;

    public record Row(
            String bunnyId,
            BigDecimal highPrice,
            BigDecimal lowPrice,
            BigDecimal tradeQuantity,
            BigDecimal closingPrice
    ) {}

    public List<Row> aggregateByBunny(LocalDateTime from, LocalDateTime to) {
        QMatch m     = QMatch.match;
        QMatch mMax  = new QMatch("mMax");  // max(createdAt) 계산용
        QMatch mTie  = new QMatch("mTie");  // 동일시각에서 max(id) 계산용
        QMatch mLast = new QMatch("mLast"); // 최종 unitPrice 조회용

        var inWindow = m.createdAt.goe(from).and(m.createdAt.lt(to));

        // 서브쿼리 1: 해당 bunny의 구간 내 "가장 늦은 createdAt"
        var maxCreatedAtSub =
                JPAExpressions.select(mMax.createdAt.max())
                        .from(mMax)
                        .where(
                                mMax.bunny.id.eq(m.bunny.id),
                                mMax.createdAt.goe(from),
                                mMax.createdAt.lt(to)
                        );

        // 서브쿼리 2: max(createdAt) 시각에 해당하는 행들 중 "id 최대값" (tie-break)
        var maxIdAtMaxCreatedSub =
                JPAExpressions.select(mTie.id.max())
                        .from(mTie)
                        .where(
                                mTie.bunny.id.eq(m.bunny.id),
                                mTie.createdAt.eq(maxCreatedAtSub),
                                mTie.createdAt.goe(from),
                                mTie.createdAt.lt(to)
                        );

        return queryFactory
                .select(Projections.constructor(Row.class,
                        // 1) bunnyId
                        m.bunny.id,
                        // 2) highPrice: MAX(unitPrice) with COALESCE
                        Expressions.numberTemplate(BigDecimal.class, "COALESCE(MAX({0}), 0)", m.unitPrice),
                        // 3) lowPrice : MIN(unitPrice) with COALESCE
                        Expressions.numberTemplate(BigDecimal.class, "COALESCE(MIN({0}), 0)", m.unitPrice),
                        // 4) tradeQty : SUM(quantity) with COALESCE
                        Expressions.numberTemplate(BigDecimal.class, "COALESCE(SUM({0}), 0)", m.quantity),
                        // 5) closingPrice: 구간 마지막 체결가 (max(createdAt) & tie-break max(id))
                        JPAExpressions.select(mLast.unitPrice)
                                .from(mLast)
                                .where(
                                        mLast.bunny.id.eq(m.bunny.id),
                                        mLast.createdAt.eq(maxCreatedAtSub),
                                        mLast.id.eq(maxIdAtMaxCreatedSub)
                                )
                ))
                .from(m)
                .where(inWindow)
                .groupBy(m.bunny.id)
                .fetch();
    }
}
