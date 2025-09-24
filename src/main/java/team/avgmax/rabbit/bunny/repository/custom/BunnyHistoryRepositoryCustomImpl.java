package team.avgmax.rabbit.bunny.repository.custom;

import com.querydsl.core.types.dsl.*;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import team.avgmax.rabbit.bunny.dto.response.ChartDataPoint;
import team.avgmax.rabbit.bunny.entity.BunnyHistory;
import team.avgmax.rabbit.bunny.entity.QBunnyHistory;
import team.avgmax.rabbit.bunny.entity.enums.ChartInterval;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

import static com.querydsl.core.types.Projections.constructor;
import static team.avgmax.rabbit.bunny.entity.QBunnyHistory.bunnyHistory;

@RequiredArgsConstructor
public class BunnyHistoryRepositoryCustomImpl implements BunnyHistoryRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    @Override
    public List<ChartDataPoint> findChartData(String bunnyId, ChartInterval interval) {
        if (bunnyId == null) return Collections.emptyList();

        return switch (interval) {
            case DAILY -> fetchDaily(bunnyId);
            case WEEKLY -> fetchWeekly(bunnyId);
            case MONTHLY -> fetchMonthly(bunnyId);
        };
    }

    private List<ChartDataPoint> fetchDaily(String bunnyId) {
        return queryFactory
                .select(constructor(ChartDataPoint.class,
                        bunnyHistory.date,
                        bunnyHistory.highPrice,
                        bunnyHistory.lowPrice,
                        bunnyHistory.closingPrice,
                        bunnyHistory.buyQuantity,
                        bunnyHistory.sellQuantity,
                        bunnyHistory.tradeQuantity
                ))
                .from(bunnyHistory)
                .where(bunnyHistory.bunnyId.eq(bunnyId))
                .orderBy(bunnyHistory.date.desc())
                .limit(7)
                .fetch();
    }

    private List<ChartDataPoint> fetchWeekly(String bunnyId) {
        NumberTemplate<Integer> weekKey =
                Expressions.numberTemplate(Integer.class, "YEARWEEK({0}, 3)", bunnyHistory.date);

        // 합계의 null 보정 (COALESCE(SUM(...), 0))
        NumberExpression<BigDecimal> buySum =
                Expressions.numberTemplate(BigDecimal.class, "COALESCE(SUM({0}), 0)", bunnyHistory.buyQuantity);
        NumberExpression<BigDecimal> sellSum =
                Expressions.numberTemplate(BigDecimal.class, "COALESCE(SUM({0}), 0)", bunnyHistory.sellQuantity);
        NumberExpression<BigDecimal> tradeSum =
                Expressions.numberTemplate(BigDecimal.class, "COALESCE(SUM({0}), 0)", bunnyHistory.tradeQuantity);

        QBunnyHistory bhMax = new QBunnyHistory("bhMax");
        QBunnyHistory bhLast = new QBunnyHistory("bhLast");

        return queryFactory
                .select(constructor(ChartDataPoint.class,
                        // 대표 날짜: 그 주의 마지막 날짜
                        bunnyHistory.date.max(),
                        // 주간 고가/저가
                        bunnyHistory.highPrice.max(),
                        bunnyHistory.lowPrice.min(),
                        // 그 주의 마지막 날짜의 종가
                        JPAExpressions.select(bhLast.closingPrice)
                                .from(bhLast)
                                .where(
                                        bhLast.bunnyId.eq(bunnyId),
                                        Expressions.numberTemplate(Integer.class, "YEARWEEK({0}, 3)", bhLast.date).eq(weekKey),
                                        bhLast.date.eq(
                                                JPAExpressions.select(bhMax.date.max())
                                                        .from(bhMax)
                                                        .where(
                                                                bhMax.bunnyId.eq(bunnyId),
                                                                Expressions.numberTemplate(Integer.class, "YEARWEEK({0}, 3)", bhMax.date).eq(weekKey)
                                                        )
                                        )
                                ),
                        // 주간 합계
                        buySum,
                        sellSum,
                        tradeSum
                ))
                .from(bunnyHistory)
                .where(bunnyHistory.bunnyId.eq(bunnyId))
                .groupBy(weekKey)
                .orderBy(weekKey.desc())
                .limit(7)
                .fetch();
    }

    private List<ChartDataPoint> fetchMonthly(String bunnyId) {
        StringTemplate monthKey =
                Expressions.stringTemplate("DATE_FORMAT({0}, '%Y-%m')", bunnyHistory.date);

        // 합계 null 보정
        NumberExpression<BigDecimal> buySum  =
                Expressions.numberTemplate(BigDecimal.class, "COALESCE(SUM({0}), 0)", bunnyHistory.buyQuantity);
        NumberExpression<BigDecimal> sellSum =
                Expressions.numberTemplate(BigDecimal.class, "COALESCE(SUM({0}), 0)", bunnyHistory.sellQuantity);
        NumberExpression<BigDecimal> tradeSum =
                Expressions.numberTemplate(BigDecimal.class, "COALESCE(SUM({0}), 0)", bunnyHistory.tradeQuantity);

        // 월 마지막 일자의 종가를 뽑기 위한 보조 별칭
        QBunnyHistory bhMax  = new QBunnyHistory("bhMax");
        QBunnyHistory bhLast = new QBunnyHistory("bhLast");

        return queryFactory
                .select(constructor(ChartDataPoint.class,
                        // 대표 날짜: 해당 달의 마지막 날짜 (메인 그룹에서 max)
                        bunnyHistory.date.max(),
                        // 월간 고가/저가
                        bunnyHistory.highPrice.max(),
                        bunnyHistory.lowPrice.min(),
                        // 그 달의 마지막 날짜의 종가
                        JPAExpressions.select(bhLast.closingPrice)
                                .from(bhLast)
                                .where(
                                        bhLast.bunnyId.eq(bunnyId),
                                        Expressions.stringTemplate("DATE_FORMAT({0}, '%Y-%m')", bhLast.date).eq(monthKey),
                                        bhLast.date.eq(
                                                JPAExpressions.select(bhMax.date.max())
                                                        .from(bhMax)
                                                        .where(
                                                                bhMax.bunnyId.eq(bunnyId),
                                                                Expressions.stringTemplate("DATE_FORMAT({0}, '%Y-%m')", bhMax.date).eq(monthKey)
                                                        )
                                        )
                                ),
                        // 월간 합계
                        buySum,
                        sellSum,
                        tradeSum
                ))
                .from(bunnyHistory)
                .where(bunnyHistory.bunnyId.eq(bunnyId))
                .groupBy(monthKey)
                .orderBy(monthKey.desc())
                .limit(7)
                .fetch();
    }

    @Override
    public List<BunnyHistory> findRecentByBunnyIdOrderByDateAsc(String bunnyId, int days) {
        if (bunnyId == null || days <= 0) return Collections.emptyList();

        LocalDate targetDate = LocalDate.now().minusDays(days);

        return queryFactory
                .selectFrom(bunnyHistory)
                .where(
                        bunnyHistory.bunnyId.eq(bunnyId),
                        bunnyHistory.date.goe(targetDate)
                )
                .orderBy(bunnyHistory.date.asc())
                .fetch();
    }
}
