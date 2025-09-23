package team.avgmax.rabbit.bunny.controller;

import org.springframework.security.oauth2.jwt.Jwt;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import team.avgmax.rabbit.bunny.dto.orderBook.OrderBookSnapshot;
import team.avgmax.rabbit.bunny.dto.request.OrderRequest;
import team.avgmax.rabbit.bunny.dto.response.ChartResponse;
import team.avgmax.rabbit.bunny.dto.response.FetchBunnyResponse;
import team.avgmax.rabbit.bunny.dto.response.OrderListResponse;
import team.avgmax.rabbit.bunny.dto.response.MyBunnyResponse;
import team.avgmax.rabbit.bunny.entity.enums.BunnyFilter;
import team.avgmax.rabbit.bunny.entity.enums.ChartInterval;
import team.avgmax.rabbit.bunny.service.BunnyService;
import team.avgmax.rabbit.bunny.dto.response.OrderResponse;
import team.avgmax.rabbit.bunny.dto.response.RabbitIndexResponse;
import team.avgmax.rabbit.user.entity.enums.Role;

import java.net.URI;
import java.util.List;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping(value = "/bunnies", produces = "application/json")
public class BunnyController implements BunnyApiDocs {

    private final BunnyService bunnyService;

    // RABBIT 지수 조회
    @GetMapping("/rabbit-index")
    public ResponseEntity<RabbitIndexResponse> getRabbitIndex() {
        log.info("GET RABBIT 지수 조회");
        return ResponseEntity.ok(bunnyService.getRabbitIndex());
    }

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

    // 특정 버니 마이 리스트 조회
    @GetMapping("/{bunnyName}/mylist")
    public ResponseEntity<OrderListResponse> getMyBunnyList(@AuthenticationPrincipal Jwt jwt, @PathVariable String bunnyName) {
        String userId = jwt.getSubject();
        log.info("GET 특정 버니 마이 리스트 조회: {}, userId={}", bunnyName, userId);

        return ResponseEntity.ok(bunnyService.getMyBunnyList(bunnyName, userId));
    }

    // 버니 좋아요 추가
    @PostMapping("/{bunnyName}/like")
    public ResponseEntity<Void> addBunnyLike(@AuthenticationPrincipal Jwt jwt, @PathVariable String bunnyName) {
        String userId = jwt.getSubject();
        
        @SuppressWarnings("unchecked")
        Role userRole = Role.valueOf(((List<String>) jwt.getClaim("roles")).get(0));
        
        log.info("POST 버니 좋아요 추가: {}, role={}", bunnyName, userRole);
        bunnyService.addBunnyLike(bunnyName, userId, userRole);
        return ResponseEntity.ok().build();
    }

    // 버니 좋아요 취소  
    @DeleteMapping("/{bunnyName}/like")
    public ResponseEntity<Void> cancelBunnyLike(@AuthenticationPrincipal Jwt jwt, @PathVariable String bunnyName) {
        String userId = jwt.getSubject();
        
        @SuppressWarnings("unchecked")
        Role userRole = Role.valueOf(((List<String>) jwt.getClaim("roles")).get(0));
        
        log.info("DELETE 버니 좋아요 취소: {}, role={}", bunnyName, userRole);
        bunnyService.cancelBunnyLike(bunnyName, userId, userRole);
        return ResponseEntity.noContent().build();
    }

    // 거래 주문 요청
    @PostMapping(value = "/{bunnyName}/orders", consumes = "application/json")
    public ResponseEntity<OrderResponse> createOrder(
            @PathVariable String bunnyName,
            @Valid @RequestBody OrderRequest request,
            @AuthenticationPrincipal Jwt jwt
    ) {
        String userId = jwt.getSubject();
        log.info("POST 거래 주문 요청: user={}, bunny={}", userId, bunnyName);


        OrderResponse response = bunnyService.createOrder(bunnyName, request, userId);
        // 차후에 절대경로를 추가해주면 좀 더 RESTful 해진다.
        URI location = URI.create("/bunnies/" + bunnyName + "/orders/" + response.orderId());

        return ResponseEntity.created(location).body(response);
    }

    // 거래 주문 취소
    @DeleteMapping("/{bunnyName}/orders/{orderId}")
    public ResponseEntity<Void> cancelOrder(
            @PathVariable String bunnyName,
            @PathVariable String orderId,
            @AuthenticationPrincipal Jwt jwt
    ) {
        String userId = jwt.getSubject();
        log.info("DELETE 거래 주문 취소 : bunny={}, orderId={}, user={}", bunnyName, orderId, userId);

        bunnyService.cancelOrder(bunnyName, orderId, userId);

        return ResponseEntity.noContent().build();
    }

    // 특정 버니 호가창 스냅샷 조회
    @GetMapping("/{bunnyName}/orderbook")
    public ResponseEntity<OrderBookSnapshot> getOrderBookSnapshot(@PathVariable String bunnyName) {
        log.info("GET 호가창 조회: {}", bunnyName);

        return ResponseEntity.ok(bunnyService.getOrderBookSnapshot(bunnyName));
    }
}
