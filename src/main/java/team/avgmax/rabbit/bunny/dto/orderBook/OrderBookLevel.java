package team.avgmax.rabbit.bunny.dto.orderBook;

import team.avgmax.rabbit.bunny.entity.enums.OrderType;

import java.math.BigDecimal;

public record OrderBookLevel(
        BigDecimal price,      // 해당 가격 레벨 대의 가격
        BigDecimal quantity,   // 해당 가격 레벨 대의 잔여량 (0보다 초과만 포함)
        OrderType type         // 주문 타입 (BUY 또는 SELL)
) {}
