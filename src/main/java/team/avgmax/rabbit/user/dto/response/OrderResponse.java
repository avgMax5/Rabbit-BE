package team.avgmax.rabbit.user.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.Builder;
import team.avgmax.rabbit.bunny.entity.Order;
import team.avgmax.rabbit.bunny.entity.enums.OrderType;

@Builder
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record OrderResponse(
        String orderId,
        String bunnyName,
        String bunnyId,
        BigDecimal quantity,
        BigDecimal unitPrice,
        OrderType orderType,
        LocalDateTime createdAt
) {
    public static OrderResponse from(Order order) {
        return OrderResponse.builder()
                .orderId(order.getId())
                .bunnyName(order.getBunny().getBunnyName())
                .bunnyId(order.getBunny().getId())
                .quantity(order.getQuantity())
                .unitPrice(order.getUnitPrice())
                .orderType(order.getOrderType())
                .createdAt(order.getCreatedAt())
                .build();
    }
}
