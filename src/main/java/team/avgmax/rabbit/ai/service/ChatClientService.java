package team.avgmax.rabbit.ai.service;

import java.util.stream.Collectors;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import team.avgmax.rabbit.bunny.entity.Bunny;
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

    public String getAiReviewOfBunny(Bunny bunny) {
        String profile = convertProfileToString(bunny.getUser());
        String indicators = convertBunnyIndicatorsToString(bunny);
        return chatClient.prompt()
                .user(u -> u
                        .text(
                            """
                                다음은 한 개발자의 프로필 정보와 해당 개발자의 버니(주식) 시장 지표입니다.

                                [스펙]
                                {profile}

                                [버니 지표]
                                {bunny_indicators}

                                요청: 위 정보를 바탕으로 이 개발자의 '버니'를 한 줄(한 문장)으로 요약하라.

                                조건:
                                - 반드시 한 줄, 한 문장으로만 작성할 것.
                                - 지표 값(숫자)은 절대 노출하지 말 것.
                                - 버니 6대 지표(reliability, growth, stability, value, popularity, balance)의 의미는 다음과 같다:
                                • reliability: 개발자의 스펙·경력·기술 역량, 시장 내 성장성과 안정성, 그리고 평판·인증·활동성을 종합해 평가한 신뢰 수준  
                                • growth: 장기 성장 가능성을 나타내는 지표로, 가격 및 가치의 연환산 성장률(CAGR)을 기반으로 계산  
                                • stability: 최근 일정 기간의 가격 변동성(위험도)을 측정하여 안정성을 평가  
                                • value: 시장 규모(시가총액)와 신뢰도를 종합한 내재 가치 지표  
                                • popularity: 시장과 커뮤니티에서 얼마나 주목받고 있는지 평가하는 지표. 거래량과 커뮤니티 반응(좋아요)을 반영  
                                • balance: 가격·성장률·안정성 측면에서 얼마나 이상적인 조화를 이루는지 평가하는 지표  
                                - 결과는 지표 값을 드러내지 않고, 이를 종합해 **투자 성향 키워드**(예: ‘안정적 성장형’, ‘고성장·고위험형’, ‘균형형’, ‘잠재력 높은 신입형’ 등 창작 가능)으로 표현할 것.
                                - 우선순위 규칙:
                                1) 경력이 있다면: 종합적 경력과 포지션·기술스택을 요약하고, 버니 지표를 반영하여 특성을 드러낼 것.
                                2) 경력이 없다면: 학력·자격증·기술스택을 중심으로 잠재력과 성장 가능성을 강조할 것.
                                - 불필요한 개인 정보(이름, 이메일, 생년월일)는 포함하지 말 것.
                                - 톤: 간결, 전문적, 투자자·채용 담당자가 바로 이해할 수 있는 어휘 사용.

                                출력: 한 줄 요약 (한 문장).
                            """)
                        .param("profile", profile)
                        .param("bunny_indicators", indicators))
                .call()
                .content();
    }

    public String getAiFeedbackOfBunny(Bunny bunny) {
        String profile = convertProfileToString(bunny.getUser());
        String indicators = convertBunnyIndicatorsToString(bunny);
        String aiReview = bunny.getAiReview();
        return chatClient.prompt()
                .user(u -> u
                        .text(
                            """
                                다음은 한 개발자의 프로필 정보, 해당 개발자의 Bunny(주식) 시장 지표, 그리고 AI Review 요약입니다.

                                [스펙]
                                {profile}

                                [버니 지표]
                                {bunny_indicators}

                                [AI Review 요약]
                                {ai_review_summary}

                                요청: 위 정보를 바탕으로 이 개발자가 자신의 Bunny 가치를 향상시키기 위해 **구체적이고 실현 가능한 개선방안**을 장문으로 작성하라.

                                조건:
                                - 분량: 반드시 3문단으로 작성하고, 전체 길이는 1000자 이내로 제한할 것
                                - 각 문단은 소제목을 붙여 체계적으로 구성할 것
                                - 내용: 스펙, 기술 역량, 경력, 학습 방향, 시장 지표별 개선 포인트(reliability, growth, stability, value, popularity, balance), 투자자/채용자가 이해할 수 있는 가치 향상 전략
                                - 수치 지표는 참고용으로 활용하되, 실제 피드백 문장에서는 숫자를 직접 표시하지 말 것
                                - 톤: 전문적이고 친절하며 설득력 있게, 마치 코치가 조언하는 스타일
                                - 반드시 3문단으로만 작성하며, 불필요한 서론·결론은 피하고 핵심 개선 전략만 담을 것
                                - 개인 정보(이름, 이메일 등)는 포함하지 말 것
                                - 문장 간 논리적 연결과 흐름을 유지하며 친절하게 안내
                                - 가능한 경우, 개선 우선순위를 명확히 제시

                                예시 전략:
                                • 기술 스택 확장 또는 심화 학습
                                • 경력/프로젝트 추가
                                • 자격증/인증 확보
                                • Bunny 시장에서의 거래 활성화나 인지도 증대
                                • 안정성·성장률·균형 개선을 위한 투자 성향 맞춤 전략
                            """
                        )
                        .param("profile", profile)
                        .param("bunny_indicators", indicators)
                        .param("ai_review_summary", aiReview))
                .call()
                .content();
    }

    private String convertProfileToString(PersonalUser user) {
        String skill = user.getSkill().isEmpty() ? "없음" : 
                user.getSkill().stream().map(Skill::getSkillName).collect(Collectors.joining(", "));
        
        String certification = user.getCertification().isEmpty() ? "없음" :
                "[" + user.getCertification().stream()
                .map(cert -> String.format("""
                        {
                            "자격증명": %s,
                            "발급 기관": %s,
                            "취득일": %s
                        }""", cert.getName(), cert.getCa(), cert.getCdate()))
                .collect(Collectors.joining(", ")) + "]";
        
        String career = user.getCareer().isEmpty() ? "없음" :
                "[" + user.getCareer().stream()
                .map(c -> String.format("""
                        {
                            "회사명": %s,
                            "직무": %s,
                            "입사일": %s,
                            "퇴사일": %s
                        }""", c.getCompanyName(), c.getPosition(), c.getStartDate(), 
                        c.getEndDate() != null ? c.getEndDate() : "재직 중"))
                .collect(Collectors.joining(", ")) + "]";
                
        String education = user.getEducation().isEmpty() ? "없음" :
                "[" + user.getEducation().stream()
                .map(edu -> String.format("""
                        {
                            "학교명": %s,
                            "전공": %s,
                            "입학일": %s,
                            "졸업일": %s
                        }""", edu.getSchoolName(), edu.getMajor(), edu.getStartDate(),
                        edu.getEndDate() != null ? edu.getEndDate() : "재학 중"))
                .collect(Collectors.joining(", ")) + "]";


        return """
        {
            "이름": %s,
            "포지션": %s,
            "기술스택": %s,
            "자격증": %s,
            "경력": %s,
            "학력": %s
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

    private String convertBunnyIndicatorsToString(Bunny bunny) {
        return """
        {
            "reliability": %d,
            "growth": %d,
            "stability": %d,
            "value": %d,
            "popularity": %d,
            "balance": %d
        }
        """.formatted(bunny.getReliability(), bunny.getGrowth(), bunny.getStability(), bunny.getValue(), bunny.getPopularity(), bunny.getBalance());
    }
}
