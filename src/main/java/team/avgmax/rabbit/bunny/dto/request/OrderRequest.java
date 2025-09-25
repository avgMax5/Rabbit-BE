package team.avgmax.rabbit.bunny.dto.request;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Digits;
import team.avgmax.rabbit.bunny.entity.Bunny;
import team.avgmax.rabbit.bunny.entity.Order;
import team.avgmax.rabbit.bunny.entity.enums.OrderType;
import team.avgmax.rabbit.bunny.exception.BunnyError;
import team.avgmax.rabbit.bunny.exception.BunnyException;
import team.avgmax.rabbit.user.entity.PersonalUser;
import team.avgmax.rabbit.user.exception.UserError;
import team.avgmax.rabbit.user.exception.UserException;

import java.math.BigDecimal;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record OrderRequest(
        @NotNull @Positive @Digits(integer = 15, fraction = 0)
        BigDecimal quantity,
        @NotNull @Positive @Digits(integer = 15, fraction = 0)
        BigDecimal unitPrice,
        @NotNull OrderType orderType
) {
    public Order toEntity(PersonalUser user, Bunny bunny) {
        if (bunny == null) throw new BunnyException(BunnyError.BUNNY_NOT_FOUND);
        if (user == null) throw new UserException(UserError.USER_NOT_FOUND);

        return Order.builder()
                .user(user)
                .bunny(bunny)
                .quantity(quantity)
                .unitPrice(unitPrice)
                .orderType(orderType)
                .build();
    }
}
