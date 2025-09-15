package team.avgmax.rabbit.bunny.controller;

import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import team.avgmax.rabbit.bunny.dto.response.ChartResponse;
import team.avgmax.rabbit.bunny.dto.response.FetchBunnyResponse;
import team.avgmax.rabbit.bunny.dto.response.MyBunnyResponse;
import team.avgmax.rabbit.bunny.entity.enums.BunnyFilter;
import team.avgmax.rabbit.bunny.entity.enums.ChartInterval;
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
    public ResponseEntity<MyBunnyResponse> getMyBunny(@AuthenticationPrincipal Jwt jwt) {
        String userId = jwt.getSubject();
        log.info("GET 마이 버니 조회: {}", userId);

        return ResponseEntity.ok(bunnyService.getMyBunny(userId));
    }

    // 거래 차트 조회
    @GetMapping("/{bunnyName}/chart")
    public ResponseEntity<ChartResponse> getChart(@PathVariable String bunnyName, @RequestParam(defaultValue = "DAILY") ChartInterval interval) {
        log.info("GET 거래 차트 조회: {}, interval: {}", bunnyName, interval);

        return ResponseEntity.ok(bunnyService.getChart(bunnyName, interval));
    }

    // 버니 좋아요 추가
    @PostMapping("/{bunnyName}/like")
    public ResponseEntity<Void> addBunnyLike(@AuthenticationPrincipal Jwt jwt, @PathVariable String bunnyName) {
        String userId = jwt.getSubject();
        log.info("POST 버니 좋아요 추가: {}", bunnyName);
        bunnyService.addBunnyLike(bunnyName, userId);
        return ResponseEntity.ok().build();
    }

    // 버니 좋아요 취소
    @DeleteMapping("/{bunnyName}/like")
    public ResponseEntity<Void> cancelBunnyLike(@AuthenticationPrincipal Jwt jwt, @PathVariable String bunnyName) {
        String userId = jwt.getSubject();
        log.info("DELETE 버니 좋아요 취소: {}", bunnyName);
        bunnyService.cancelBunnyLike(bunnyName, userId);
        return ResponseEntity.noContent().build();
    }
}
