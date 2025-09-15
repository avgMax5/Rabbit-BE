package team.avgmax.rabbit.ai.dto.response;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.Builder;

@Builder
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record ScoreResponse (
        int growth,
        int stability,
        int value,
        int popularity,
        int balance
) {
}