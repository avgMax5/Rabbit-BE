package team.avgmax.rabbit.funding.dto.data;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.Builder;
import team.avgmax.rabbit.user.entity.PersonalUser;

import java.math.BigDecimal;

@Builder
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record UserFundingSummary(
        PersonalUser user,
        BigDecimal totalQuantity,
        String bunnyName
) {
}