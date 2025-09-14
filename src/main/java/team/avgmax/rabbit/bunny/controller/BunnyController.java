package team.avgmax.rabbit.bunny.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import team.avgmax.rabbit.auth.oauth2.CustomOAuth2User;
import team.avgmax.rabbit.bunny.dto.response.FetchBunnyResponse;
import team.avgmax.rabbit.bunny.dto.response.MyBunnyResponse;
import team.avgmax.rabbit.bunny.entity.enums.BunnyFilter;
import team.avgmax.rabbit.bunny.service.BunnyService;

import java.util.List;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/bunnies")
public class BunnyController {

    private final BunnyService bunnyService;

    // 버니 목록 조회
    @GetMapping
    public ResponseEntity<List<FetchBunnyResponse>> getBunnyList(@RequestParam(required = false) String filter) {
        log.info("GET 버니 목록 조회");
        BunnyFilter bunnyFilter = BunnyFilter.fromValue(filter); // IllegalAccessException 발생 시 GlobalException 에서 처리

        return ResponseEntity.ok(bunnyService.getBunniesByFilter(bunnyFilter));
    }

    // 버니 상세 조회
    @GetMapping("/{bunnyName}")
    public ResponseEntity<FetchBunnyResponse> getBunny(@PathVariable String bunnyName) {
        log.info("GET 버니 상세 조회: {}",bunnyName);

        return ResponseEntity.ok(bunnyService.getBunnyByName(bunnyName));
    }

    // 마이 버니 조회
    @GetMapping("/me")
    public ResponseEntity<MyBunnyResponse> getMyBunny(@AuthenticationPrincipal CustomOAuth2User customOAuth2User) {
        log.info("GET 마이 버니 조회: {}", customOAuth2User.getName());

        return ResponseEntity.ok(bunnyService.getMyBunny(customOAuth2User));
    }
}
