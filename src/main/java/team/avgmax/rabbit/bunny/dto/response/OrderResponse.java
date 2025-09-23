package team.avgmax.rabbit.bunny.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import team.avgmax.rabbit.bunny.entity.enums.OrderType;
import team.avgmax.rabbit.bunny.entity.Order;

import lombok.Builder;
import team.avgmax.rabbit.global.policy.FeePolicy;

@Builder
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record OrderResponse(
    String orderId,
    String bunnyName,
    String bunnyId,
    BigDecimal quantity,
    BigDecimal unitPrice,
    OrderType orderType,
    BigDecimal totalAmount,  // 수수료 전 총 주문 금액
    BigDecimal fee,          // 예상 수수료
    BigDecimal finalAmount, // 수수료 적용 후 금액
    LocalDateTime orderedAt
) {
    public static OrderResponse from(Order order) {
        BigDecimal total = order.getQuantity().multiply(order.getUnitPrice());
        BigDecimal fee = FeePolicy.calcFee(total);

        BigDecimal finalAmount = (order.getOrderType() == OrderType.BUY) ? total.add(fee) : total.subtract(fee);

        return OrderResponse.builder()
                .orderId(order.getId())
                .bunnyName(order.getBunny().getBunnyName())
                .bunnyId(order.getBunny().getId())
                .quantity(order.getQuantity())
                .unitPrice(order.getUnitPrice())
                .orderType(order.getOrderType())
                .totalAmount(total)
                .fee(fee)
                .finalAmount(finalAmount)
                .orderedAt(LocalDateTime.now())
                .build();
    }
}
