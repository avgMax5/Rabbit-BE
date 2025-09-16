package team.avgmax.rabbit.bunny.repository.custom;

import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import team.avgmax.rabbit.bunny.entity.QMatch;
import team.avgmax.rabbit.bunny.entity.enums.OrderType;

import java.math.BigDecimal;

@Repository
@RequiredArgsConstructor
public class MatchRepositoryCustomImpl implements MatchRepositoryCustom{
    private final JPAQueryFactory queryFactory;

    public BigDecimal sumFilledByUserSideAndPrice(String bunnyId, String userId, OrderType side, BigDecimal price) {
        QMatch match = QMatch.match;
        BooleanExpression userSideExpr =
                (side == OrderType.BUY) ? match.buyUser.id.eq(userId) : match.sellUser.id.eq(userId);

        BigDecimal sum = queryFactory.select(match.quantity.sum())
                .from(match)
                .where(
                        match.bunny.id.eq(bunnyId),
                        match.unitPrice.eq(price),
                        userSideExpr
                )
                .fetchOne();

        return sum != null ? sum : BigDecimal.ZERO;
    }
}
