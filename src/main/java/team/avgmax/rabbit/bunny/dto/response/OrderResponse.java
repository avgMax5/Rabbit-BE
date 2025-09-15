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
    LocalDateTime createdAt,
    OrderType type,
    BigDecimal quantity,
    BigDecimal unitPrice,
    BigDecimal totalAmount
) {
    public static OrderResponse from(Order order) {
        return OrderResponse.builder()
                .orderId(order.getId())
                .createdAt(order.getCreatedAt())
                .type(order.getOrderType())
                .quantity(order.getQuantity())
                .unitPrice(order.getUnitPrice())
                .totalAmount(order.getQuantity().multiply(order.getUnitPrice())) // 수수료 고려해야됨
                .build();
    }
}
