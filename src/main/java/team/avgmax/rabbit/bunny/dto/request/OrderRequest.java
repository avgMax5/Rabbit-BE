package team.avgmax.rabbit.bunny.dto.request;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import team.avgmax.rabbit.bunny.entity.Bunny;
import team.avgmax.rabbit.bunny.entity.Order;
import team.avgmax.rabbit.bunny.entity.enums.OrderType;
import team.avgmax.rabbit.user.entity.PersonalUser;

import java.math.BigDecimal;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record OrderRequest(
        @NotNull @Positive BigDecimal quantity,
        @NotNull @Positive BigDecimal unitPrice,
        @NotNull OrderType orderType
) {
    public Order toEntity(PersonalUser user, Bunny bunny) {
        if (user == null || bunny == null) throw new IllegalArgumentException("user / bunny 는 필수 입니다..");

        return Order.builder()
                .user(user)
                .bunny(bunny)
                .quantity(quantity)
                .unitPrice(unitPrice)
                .orderType(orderType)
                .build();
    }
}
