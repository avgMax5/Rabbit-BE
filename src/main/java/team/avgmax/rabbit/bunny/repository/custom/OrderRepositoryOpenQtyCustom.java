package team.avgmax.rabbit.bunny.repository.custom;

import team.avgmax.rabbit.bunny.entity.enums.OrderType;

import java.math.BigDecimal;

public interface OrderRepositoryOpenQtyCustom {

    BigDecimal sumOpenQuantityByBunnyAndSide(String bunnyId, OrderType side);
}
