package team.avgmax.rabbit.bunny.dto.response;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.Builder;

import java.math.BigDecimal;

@Builder
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record BunnyUserContextResponse(
    boolean isLiked,
    BigDecimal buyableAmount,
    BigDecimal sellableQuantity
) {
    public static BunnyUserContextResponse of(boolean isLiked, BigDecimal buyableAmount, BigDecimal sellableQuantity) {
        return BunnyUserContextResponse.builder()
                .isLiked(isLiked)
                .buyableAmount(buyableAmount)
                .sellableQuantity(sellableQuantity)
                .build();
    }
}
