package team.avgmax.rabbit.bunny.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import team.avgmax.rabbit.bunny.entity.enums.OrderType;
import team.avgmax.rabbit.bunny.entity.Order;

import lombok.Builder;

@Builder
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record OrderResponse(
    String orderId,
    String bunnyName,
    String bunnyId,
    BigDecimal quantity,
    BigDecimal unitPrice,
    OrderType orderType,
    BigDecimal totalAmount,
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
                .totalAmount(order.getQuantity().multiply(order.getUnitPrice())) // 수수료 고려해야됨
                .createdAt(order.getCreatedAt())
                .build();
    }
}
