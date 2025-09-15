package team.avgmax.rabbit.bunny.controller;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import team.avgmax.rabbit.auth.oauth2.CustomOAuth2User;
import team.avgmax.rabbit.bunny.dto.request.OrderRequest;
import team.avgmax.rabbit.bunny.dto.response.ChartResponse;
import team.avgmax.rabbit.bunny.dto.response.FetchBunnyResponse;
import team.avgmax.rabbit.bunny.dto.response.MyBunnyResponse;
import team.avgmax.rabbit.bunny.entity.enums.BunnyFilter;
import team.avgmax.rabbit.bunny.entity.enums.ChartInterval;
import team.avgmax.rabbit.bunny.service.BunnyService;
import team.avgmax.rabbit.user.dto.response.OrderResponse;
import team.avgmax.rabbit.user.entity.PersonalUser;

import java.net.URI;
import java.util.List;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping(value = "/bunnies", produces = "application/json")
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
    public ResponseEntity<MyBunnyResponse> getMyBunny(@AuthenticationPrincipal CustomOAuth2User user) {
        log.info("GET 마이 버니 조회: {}", user.getName());

        return ResponseEntity.ok(bunnyService.getMyBunny(user));
    }

    // 거래 차트 조회
    @GetMapping("/{bunnyName}/chart")
    public ResponseEntity<ChartResponse> getChart(@PathVariable String bunnyName, @RequestParam(defaultValue = "DAILY") ChartInterval interval) {
        log.info("GET 거래 차트 조회: {}, interval: {}", bunnyName, interval);

        return ResponseEntity.ok(bunnyService.getChart(bunnyName, interval));
    }

    // 거래 주문 요청
    @PostMapping(value = "/{bunnyName}/orders", consumes = "application/json")
    public ResponseEntity<OrderResponse> createOrder(
            @PathVariable String bunnyName,
            @Valid @RequestBody OrderRequest request,
            @AuthenticationPrincipal CustomOAuth2User user
    ) {
        log.info("POST 거래 주문 요청: user={}, bunny={}", user.getName(), bunnyName);

        PersonalUser personalUser = user.getPersonalUser();
        OrderResponse response = bunnyService.createOrder(bunnyName, request, personalUser);
        // 차후에 절대경로를 추가해주면 좀 더 RESTful 해진다.
        URI location = URI.create("/bunnies/" + bunnyName + "/orders/" + response.orderId());

        return ResponseEntity.created(location).body(response);
    }
}
