package team.avgmax.rabbit.bunny.dto.response;

import java.util.List;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import lombok.Builder;
import team.avgmax.rabbit.bunny.dto.data.BunnyPressureData;

@Builder
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record PressureResponse(
        List<BunnyPressureData> buyTop5,
        List<BunnyPressureData> sellTop5
) {
    public static PressureResponse of(
            List<BunnyPressureData> buyTop5,
            List<BunnyPressureData> sellTop5
    ) {
        return PressureResponse.builder()
                .buyTop5(buyTop5)
                .sellTop5(sellTop5)
                .build();
    }
}