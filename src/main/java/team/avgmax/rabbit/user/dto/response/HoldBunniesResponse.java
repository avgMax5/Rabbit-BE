package team.avgmax.rabbit.user.dto.response;

import java.util.List;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import lombok.Builder;

@Builder
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record HoldBunniesResponse(
    List<HoldBunnyResponse> holdBunnies
) {
    public static HoldBunniesResponse from(List<HoldBunnyResponse> holdBunnies) {
        return HoldBunniesResponse.builder()
            .holdBunnies(holdBunnies)
            .build();
    }
}
