package team.avgmax.rabbit.user.dto.response;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import lombok.Builder;
import team.avgmax.rabbit.user.entity.PersonalUser;

@Builder
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record FetchUserResponse (
    String userId,
    String name,
    String image,
    String role,
    String carrot,
    String myBunnyName
) {
    public static FetchUserResponse from(PersonalUser personalUser, String myBunnyName) {
        return FetchUserResponse.builder()
                .userId(personalUser.getId())
                .name(personalUser.getName())
                .image(personalUser.getImage())
                .role(personalUser.getRole().name())
                .carrot(personalUser.getCarrot().toString())
                .myBunnyName(myBunnyName)
                .build();
    }
}