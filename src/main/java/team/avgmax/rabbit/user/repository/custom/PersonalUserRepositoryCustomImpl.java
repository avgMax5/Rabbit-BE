package team.avgmax.rabbit.user.repository.custom;

import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import team.avgmax.rabbit.user.entity.QPersonalUser;

import java.math.BigDecimal;

@Repository
@RequiredArgsConstructor
public class PersonalUserRepositoryCustomImpl implements PersonalUserRepositoryCustom{

    private final JPAQueryFactory queryFactory;

    @Override
    public void addCarrotForUpdate(String userId, BigDecimal delta) {
        QPersonalUser user = QPersonalUser.personalUser;

        queryFactory.update(user)
                .set(user.carrot, user.carrot.add(delta))
                .where(user.id.eq(userId))
                .execute();
    }
}
