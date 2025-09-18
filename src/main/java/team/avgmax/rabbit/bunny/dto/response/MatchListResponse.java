package team.avgmax.rabbit.bunny.dto.response;

import java.util.List;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import lombok.Builder;

@Builder
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record MatchListResponse(
    long size,
    List<MatchResponse> matches
) {
    public static MatchListResponse from(List<MatchResponse> matches) {
        return MatchListResponse.builder()
                .size(matches.size())
                .matches(matches)
                .build();
    }
}