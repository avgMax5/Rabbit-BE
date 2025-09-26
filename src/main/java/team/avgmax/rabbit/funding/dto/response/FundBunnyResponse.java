package team.avgmax.rabbit.funding.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import lombok.Builder;

import team.avgmax.rabbit.bunny.entity.enums.BunnyType;
import team.avgmax.rabbit.funding.entity.FundBunny;

@Builder
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record FundBunnyResponse(
    String fundBunnyId,
    String bunnyName,
    String image,
    BunnyType bunnyType,
    BigDecimal targetBny,
    BigDecimal collectedBny,
    BigDecimal remainingBny,
    LocalDateTime createdAt,
    LocalDateTime endAt
) {
    public static FundBunnyResponse from(FundBunny fundBunny) {
        return FundBunnyResponse.builder()
                .fundBunnyId(fundBunny.getId())
                .bunnyName(fundBunny.getBunnyName())
                .image(fundBunny.getUser().getImage())
                .bunnyType(fundBunny.getType())
                .targetBny(fundBunny.getType().getTotalSupply())
                .collectedBny(fundBunny.getCollectedBny())
                .remainingBny(fundBunny.getType().getTotalSupply().subtract(fundBunny.getCollectedBny()))
                .createdAt(fundBunny.getCreatedAt())
                .endAt(fundBunny.getEndAt())
                .build();
    }

    public static List<FundBunnyResponse> from(List<FundBunny> fundBunnies) {
        return fundBunnies.stream()
                .map(FundBunnyResponse::from)
                .toList();
    }
}
