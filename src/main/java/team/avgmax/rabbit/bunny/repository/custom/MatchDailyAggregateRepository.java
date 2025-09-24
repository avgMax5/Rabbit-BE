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
        QMatch match = QMatch.match;
        QMatch matchMaxTime = new QMatch("matchMaxTime");
        QMatch tieBreaker = new QMatch("tieBreaker");
        QMatch lastMatch = new QMatch("lastMatch");

        // 기간 필터
        var inWindow = match.createdAt.goe(from).and(match.createdAt.lt(to));

        // bunny 구간 내 가장 늦은 "createdAt"
        var maxCreatedAtQuery = JPAExpressions
                .select(matchMaxTime.createdAt.max())
                .from(matchMaxTime)
                .where(
                        matchMaxTime.bunny.id.eq(match.bunny.id),
                        matchMaxTime.createdAt.goe(from),
                        matchMaxTime.createdAt.lt(to)
                );

        // maxCreatedAtQuery 의 createdAt 에서 "가장 큰 id"
        var idMaxAtMaxCreatedQuery = JPAExpressions
                .select(tieBreaker.id.max())
                .from(tieBreaker)
                .where(
                        tieBreaker.bunny.id.eq(match.bunny.id),
                        tieBreaker.createdAt.eq(maxCreatedAtQuery),
                        tieBreaker.createdAt.goe(from),
                        tieBreaker.createdAt.lt(to)
                );

        // 모든 버니 집계(aggregation), COALESCE 로 Null 방지
        return queryFactory
                .select(
                        Projections.constructor(
                                Row.class,
                                // bunny 식별자
                                match.bunny.id,
                                // 고가
                                Expressions.numberTemplate(BigDecimal.class, "COALESCE(MAX({0}), 0)", match.unitPrice),
                                // 저가
                                Expressions.numberTemplate(BigDecimal.class, "COALESCE(MIN({0}), 0)", match.unitPrice),
                                // 총 거래량(체결량)
                                Expressions.numberTemplate(BigDecimal.class, "COALESCE(SUM({0}), 0)", match.quantity),
                                // 종가
                                JPAExpressions
                                        .select(lastMatch.unitPrice)
                                        .from(lastMatch)
                                        .where(
                                                lastMatch.bunny.id.eq(match.bunny.id),
                                                lastMatch.createdAt.eq(maxCreatedAtQuery),
                                                lastMatch.id.eq(idMaxAtMaxCreatedQuery)
                                        )
                        )
                )
                .from(match)
                .where(inWindow)
                .groupBy(match.bunny.id)
                .fetch();
    }
}
