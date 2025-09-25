package team.avgmax.rabbit.bunny.repository.custom;

import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import team.avgmax.rabbit.bunny.entity.QOrder;
import team.avgmax.rabbit.bunny.entity.enums.OrderType;

import java.math.BigDecimal;

@Repository
@RequiredArgsConstructor
public class OrderRepositoryOpenQtyCustomImpl implements OrderRepositoryOpenQtyCustom{

    private final JPAQueryFactory queryFactory;

    @Override
    public BigDecimal sumOpenQuantityByBunnyAndSide(String bunnyId, OrderType side) {
        QOrder order = QOrder.order;

        BigDecimal sum = queryFactory
                .select(order.quantity.sum())
                .from(order)
                .where(
                        order.bunny.id.eq(bunnyId),
                        order.orderType.eq(side),
                        order.quantity.gt(BigDecimal.ZERO)  // 왜 이게 들어갔는가?
                )
                .fetchOne();

        return (sum != null) ? sum : BigDecimal.ZERO;
    }
}
