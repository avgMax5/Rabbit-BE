package team.avgmax.rabbit.user.dto.request;

import java.time.LocalDate;
import java.util.List;

import team.avgmax.rabbit.user.entity.enums.Position;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record UpdatePersonalUserRequest(
    String name,
    LocalDate birthdate,
    String image,
    String email,
    String resume,
    String portfolio,
    List<SnsRequest> link,
    Position position,
    List<EducationRequest> education,
    List<CareerRequest> career,
    List<CertificationRequest> certification,
    List<String> skill
) {
}