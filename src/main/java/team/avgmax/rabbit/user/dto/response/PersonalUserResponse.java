package team.avgmax.rabbit.user.dto.response;

import java.time.LocalDate;
import java.util.List;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.Builder;
import team.avgmax.rabbit.user.entity.PersonalUser;
import team.avgmax.rabbit.user.entity.Skill;

@Builder
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record PersonalUserResponse(
        String userId,
        String name,
        LocalDate birthdate,
        String image,
        String email,
        String resume,
        String portfolio,
        List<SnsResponse> link,
        String position,
        List<EducationResponse> education,
        List<CareerResponse> career,
        List<CertificationResponse> certification,
        List<String> skill,
        String aiReview
) {
    public static PersonalUserResponse from(PersonalUser personalUser) {
        return PersonalUserResponse.builder()
                .userId(personalUser.getId())
                .name(personalUser.getName())
                .birthdate(personalUser.getBirthdate())
                .image(personalUser.getImage())
                .email(personalUser.getEmail())
                .resume(personalUser.getResume())
                .portfolio(personalUser.getPortfolio())
                .link(SnsResponse.from(personalUser.getSns()))
                .position(personalUser.getPosition().name())
                .education(EducationResponse.from(personalUser.getEducation()))
                .career(CareerResponse.from(personalUser.getCareer()))
                .certification(CertificationResponse.from(personalUser.getCertification()))
                .skill(personalUser.getSkill().stream()
                        .map(Skill::getSkillName)
                        .toList())
                .aiReview(personalUser.getAiReview())
                .build();
    }
}