package team.avgmax.rabbit.ai.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import team.avgmax.rabbit.ai.dto.response.ScoreResponse;
import team.avgmax.rabbit.ai.service.ChatModelService;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/ai")
public class ChatModelController implements ChatModelApiDocs {

    private final ChatModelService chatModelService;

    @GetMapping("/ask")
    public ResponseEntity<String> ask(@AuthenticationPrincipal Jwt jwt, @RequestParam String answer) {
        String userId = jwt.getSubject();
        log.info("GET /ai/ask: userId={}", userId);
        return ResponseEntity.ok(chatModelService.ask(answer));
    }

    @GetMapping("/score")
    public ResponseEntity<ScoreResponse> score(@AuthenticationPrincipal Jwt jwt) {
        String personalUserId = jwt.getSubject();
        log.info("GET /ai/score: userId={}", personalUserId);

        return ResponseEntity.ok(chatModelService.score(personalUserId));
    }
}
