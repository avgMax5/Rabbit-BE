package team.avgmax.rabbit.funding.repository;

import com.querydsl.core.types.Projections;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import team.avgmax.rabbit.funding.dto.data.UserFundingSummary;
import team.avgmax.rabbit.funding.entity.FundBunny;
import team.avgmax.rabbit.funding.entity.QFunding;
import team.avgmax.rabbit.user.entity.PersonalUser;
import team.avgmax.rabbit.user.entity.QPersonalUser;
import team.avgmax.rabbit.bunny.entity.QBunny;

import java.math.BigDecimal;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class FundingRepositoryCustomImpl implements FundingRepositoryCustom {
    
    private final JPAQueryFactory queryFactory;
    
    @Override
    public List<UserFundingSummary> findUserFundingSummariesByFundBunnyOrderByQuantityDesc(FundBunny fundBunny) {
        QFunding funding = QFunding.funding;
        QPersonalUser user = QPersonalUser.personalUser;
        QBunny bunny = QBunny.bunny;
        
        return queryFactory
                .select(Projections.constructor(
                        UserFundingSummary.class,
                        user,
                        funding.quantity.sum(),
                        bunny.bunnyName
                ))
                .from(funding)
                .join(funding.user, user)
                .leftJoin(bunny).on(bunny.user.eq(user))
                .where(funding.fundBunny.eq(fundBunny))
                .groupBy(user, bunny.bunnyName)
                .orderBy(funding.quantity.sum().desc())
                .fetch();
    }
    
    @Override
    public BigDecimal findTotalQuantityByUserAndFundBunny(PersonalUser user, FundBunny fundBunny) {
        QFunding funding = QFunding.funding;
        
        BigDecimal result = queryFactory
                .select(funding.quantity.sum())
                .from(funding)
                .where(funding.user.eq(user)
                        .and(funding.fundBunny.eq(fundBunny)))
                .fetchOne();
        
        return result != null ? result : BigDecimal.ZERO;
    }
}
