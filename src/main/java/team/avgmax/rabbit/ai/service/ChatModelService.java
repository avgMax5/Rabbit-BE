package team.avgmax.rabbit.ai.service;


import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.converter.BeanOutputConverter;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import team.avgmax.rabbit.ai.dto.response.ScoreResponse;
import team.avgmax.rabbit.user.entity.PersonalUser;
import team.avgmax.rabbit.user.entity.Skill;

import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class ChatModelService {

    private final ChatModel chatModel;

    public String ask(String q) {
        UserMessage userMessage = new UserMessage(q);
        Prompt prompt = new Prompt(userMessage);
        ChatResponse response = chatModel.call(prompt);

        return response.getResult().getOutput().getText();
    }

    public ScoreResponse score(PersonalUser personalUser) {

        BeanOutputConverter<ScoreResponse> responseConverter =
                new BeanOutputConverter<>(ScoreResponse.class);
        String responseSchema = responseConverter.getJsonSchema();

        SystemMessage systemMessage = new SystemMessage(
                "질문 내용에 있는 대상의 가치를 5가지 항목으로 평가하고 각각 0에서 100 사이의 점수로 점수를 부여해줘. \n\n"
                        + "아래 JSON 스키마를 준수해서 반드시 JSON으로 반환해야 함.\n\n"
                        + responseSchema
        );

        // Education 리스트 → 문자열
        String educationStr = personalUser.getEducation().stream()
                .map(e -> String.format("%s %s (%s ~ %s)",
                        e.getSchoolName(),
                        e.getMajor(),
                        e.getStartDate(),
                        e.getEndDate() != null ? e.getEndDate() : "재학 중"))
                .collect(Collectors.joining("; "));

        // Career 리스트 → 문자열
        String careerStr = personalUser.getCareer().stream()
                .map(c -> String.format("%s - %s (%s ~ %s)",
                        c.getCompanyName(),
                        c.getPosition(),
                        c.getStartDate(),
                        c.getEndDate() != null ? c.getEndDate() : "재직 중"))
                .collect(Collectors.joining("; "));

        // Skill 리스트 → 문자열
        String skillsStr = personalUser.getSkill().stream()
                .map(Skill::getSkillName)
                .collect(Collectors.joining(", "));

        // JSON 생성
        String userJson = """
        {
          "education": "%s",
          "portfolio": "%s",
          "career": "%s",
          "skills": "%s"
        }
        """.formatted(
                educationStr,
                personalUser.getPortfolio(),
                careerStr,
                skillsStr
            );

        // 5. AI 호출
        UserMessage userMessage = new UserMessage(userJson);
        Prompt prompt = new Prompt(systemMessage, userMessage);
        ChatResponse response = chatModel.call(prompt);

        String responseText = response.getResult().getOutput().getText();
        if (responseText == null) {
            throw new IllegalStateException("AI 응답이 null입니다.");
        }
        return responseConverter.convert(responseText);
    }
}