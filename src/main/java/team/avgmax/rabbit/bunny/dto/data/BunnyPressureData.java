package team.avgmax.rabbit.bunny.dto.data;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import lombok.Builder;


@Builder
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record BunnyPressureData(
        String bunnyId,
        String bunnyName,
        LocalDate date,
        BigDecimal pressure
) {
    public static BunnyPressureData of(String bunnyId, String bunnyName, LocalDate date, BigDecimal pressure) {
        return BunnyPressureData.builder()
                .bunnyId(bunnyId)
                .bunnyName(bunnyName)
                .date(date)
                .pressure(pressure)
                .build();
    }
}