package team.avgmax.rabbit.bunny.dto.response;

import java.util.List;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import lombok.Builder;
import team.avgmax.rabbit.bunny.entity.Bunny;

@Builder
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)   
public record BadgeHolderListResponse(
    long size,
    String badgeName,
    String badgeImg,
    List<BadgeHolderResponse> badgeHolders
) {
    @Builder
    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record BadgeHolderResponse(
        String bunnyId,
        String bunnyName,
        String image
    ) {
        public static BadgeHolderResponse from(Bunny bunny) {
            return BadgeHolderResponse.builder()
                .bunnyId(bunny.getId())
                .bunnyName(bunny.getBunnyName())
                .image(bunny.getUser().getImage())
                .build();
        }
    }

    public static BadgeHolderListResponse from(String badgeImg, List<Bunny> bunnies) {
        return BadgeHolderListResponse.builder()
                .size(bunnies.size())
                .badgeName(badgeImg.toLowerCase())
                .badgeImg(badgeImg)
                .badgeHolders(bunnies.stream()
                        .map(BadgeHolderResponse::from)
                        .toList())
                .build();
    }
}
