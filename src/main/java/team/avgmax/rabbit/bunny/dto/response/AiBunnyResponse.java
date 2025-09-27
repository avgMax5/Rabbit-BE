package team.avgmax.rabbit.bunny.dto.response;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import lombok.Builder;
import team.avgmax.rabbit.bunny.entity.Bunny;

@Builder
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record AiBunnyResponse(
    String aiReview,
    String aiFeedback
) {
    public static AiBunnyResponse from(Bunny bunny) {
        return AiBunnyResponse.builder()
            .aiReview(bunny.getAiReview())
            .aiFeedback(bunny.getAiFeedback())
            .build();
    }
}
