package team.avgmax.rabbit.bunny.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import lombok.Builder;
import team.avgmax.rabbit.bunny.entity.Match;

import team.avgmax.rabbit.global.policy.FeePolicy;

@Builder
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record MatchResponse(
    String matchId,
    String bunnyName,
    BigDecimal quantity,
    BigDecimal unitPrice,
    BigDecimal totalAmount,
    BigDecimal fee,
    LocalDateTime matchedAt

) {
    public static MatchResponse from(Match match, String personalUserId) {
        BigDecimal total = match.getQuantity().multiply(match.getUnitPrice());
        BigDecimal fee = FeePolicy.calcFee(total);

        // String orderType = "";
        // if (match.getSellUser().getId().equals(personalUserId)) {
        //     orderType = "SELL";
        // } else if (match.getBuyUser().getId().equals(personalUserId)) {
        //     orderType = "BUY";
        // }

        return MatchResponse.builder()
                .matchId(match.getId())
                .bunnyName(match.getBunny().getBunnyName())
                .quantity(match.getQuantity())
                .unitPrice(match.getUnitPrice())
                .totalAmount(total.add(fee))
                .fee(fee)
                .matchedAt(match.getCreatedAt())
                .build();
    }
}