package team.avgmax.rabbit.ai.service;

import java.util.stream.Collectors;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import team.avgmax.rabbit.user.entity.PersonalUser;
import team.avgmax.rabbit.user.entity.Skill;

@Service
@RequiredArgsConstructor
public class ChatClientService {
    private final ChatClient chatClient;

    public String getAiReviewOfUserProfile(PersonalUser user) {
        String profile = convertProfileToString(user);
        return chatClient.prompt()
                .user(u -> u
                        .text(
                            """
                                다음은 한 개발자의 프로필 정보입니다.
                                {profile}
                                위 정보를 바탕으로 이 개발자의 역량과 강점을 한 줄로 요약해 주세요.
                                조건:
                                - 반드시 한 줄로 작성할 것
                                - 우선순위 규칙:
                                1) 경력이 있다면, 종합적 경력 내용을 작성하고 필요시 기술스택과 포지션을 보완적으로 언급할 것. 그 외 정보는 불필요함.
                                2) 경력이 없다면, 기술스택, 자격증, 학력을 종합적으로 평가해서 작성할 것
                                - 채용 담당자가 빠르게 이해할 수 있는 간결하고 전문적이고 친절한 표현으로 작성할 것
                                - 불필요한 수식어나 개인 정보(이름, 생년월일, 이메일)는 포함하지 말 것
                            """)
                        .param("profile", profile))
                .call()
                .content();
    }

    public String convertProfileToString(PersonalUser user) {
        String skill = user.getSkill().isEmpty() ? "없음" : 
                user.getSkill().stream().map(Skill::getSkillName).collect(Collectors.joining(", "));
        
        String certification = user.getCertification().isEmpty() ? "없음" :
                "[" + user.getCertification().stream()
                .map(cert -> String.format("""
                        {
                            자격증명: %s,
                            발급 기관: %s,
                            취득일: %s
                        }""", cert.getName(), cert.getCa(), cert.getCdate()))
                .collect(Collectors.joining(", ")) + "]";
        
        String career = user.getCareer().isEmpty() ? "없음" :
                "[" + user.getCareer().stream()
                .map(c -> String.format("""
                        {
                            회사명: %s,
                            직무: %s,
                            입사일: %s,
                            퇴사일: %s
                        }""", c.getCompanyName(), c.getPosition(), c.getStartDate(), 
                        c.getEndDate() != null ? c.getEndDate() : "재직 중"))
                .collect(Collectors.joining(", ")) + "]";
                
        String education = user.getEducation().isEmpty() ? "없음" :
                "[" + user.getEducation().stream()
                .map(edu -> String.format("""
                        {
                            학교명: %s,
                            전공: %s,
                            입학일: %s,
                            졸업일: %s
                        }""", edu.getSchoolName(), edu.getMajor(), edu.getStartDate(),
                        edu.getEndDate() != null ? edu.getEndDate() : "재학 중"))
                .collect(Collectors.joining(", ")) + "]";


        return """
        {
            이름: %s,
            포지션: %s,
            기술스택: %s,
            자격증: %s,
            경력: %s,
            학력: %s
        }
        """.formatted(
            user.getName(),
            user.getPosition(),
            skill,
            certification,
            career,
            education
        );
    }
}
