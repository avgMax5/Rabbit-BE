package team.avgmax.rabbit.ai.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import team.avgmax.rabbit.ai.dto.response.ScoreResponse;
import team.avgmax.rabbit.ai.service.ChatModelService;
import team.avgmax.rabbit.auth.oauth2.CustomOAuth2User;
import team.avgmax.rabbit.user.entity.PersonalUser;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/ai")
public class ChatModelController implements ChatModelApiDocs {

    private final ChatModelService chatModelService;

    @GetMapping("/ask")
    public ResponseEntity<String> ask(@AuthenticationPrincipal CustomOAuth2User customOAuth2User, @RequestParam String answer) {
        String userId = customOAuth2User.getPersonalUser().getId();
        log.info("GET /ai/ask: userId={}", userId);
        return ResponseEntity.ok(chatModelService.ask(answer));
    }

    @GetMapping("/score")
    public ResponseEntity<ScoreResponse> score(@AuthenticationPrincipal CustomOAuth2User customOAuth2User) {
        PersonalUser personalUser = customOAuth2User.getPersonalUser();
        log.info("GET /ai/score: userId={}", personalUser.getId());

        return ResponseEntity.ok(chatModelService.score(personalUser));
    }
}
