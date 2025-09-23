package team.avgmax.rabbit.bunny.repository.custom;

import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import team.avgmax.rabbit.bunny.dto.response.MatchListResponse;
import team.avgmax.rabbit.bunny.dto.response.MatchResponse;
import team.avgmax.rabbit.bunny.entity.Match;
import team.avgmax.rabbit.bunny.entity.QMatch;
import team.avgmax.rabbit.bunny.entity.enums.OrderType;

import java.math.BigDecimal;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class MatchRepositoryCustomImpl implements MatchRepositoryCustom{
    private final JPAQueryFactory queryFactory;

    public List<Match> findMatchesByUserId(String userId) {
        QMatch match = QMatch.match;
        return queryFactory.selectFrom(match)
                .where(match.buyUser.id.eq(userId).or(match.sellUser.id.eq(userId)))
                .orderBy(match.createdAt.desc())
                .fetch();
    }

    @Override
    public BigDecimal findLastTradePriceByBunnyId(String bunnyId) {
        QMatch m = QMatch.match;

        // createdAt desc, id desc 로 최신 체결 1건의 가격
        return queryFactory
                .select(m.unitPrice)
                .from(m)
                .where(m.bunny.id.eq(bunnyId))
                .orderBy(m.createdAt.desc(), m.id.desc())
                .fetchFirst();
    }
}
