package team.avgmax.rabbit.user.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import team.avgmax.rabbit.user.dto.request.UpdatePersonalUserRequest;
import team.avgmax.rabbit.user.dto.response.CarrotsResponse;
import team.avgmax.rabbit.user.dto.response.FetchUserResponse;
import team.avgmax.rabbit.user.dto.response.HoldBunniesResponse;
import team.avgmax.rabbit.user.dto.response.HoldBunniesStatsResponse;
import team.avgmax.rabbit.user.dto.response.PersonalUserResponse;
import team.avgmax.rabbit.bunny.dto.response.MatchListResponse;
import team.avgmax.rabbit.bunny.dto.response.OrderListResponse;
import team.avgmax.rabbit.user.service.FileService;
import team.avgmax.rabbit.user.service.PersonalUserService;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/personal")
public class PersonalUserController implements PersonalUserApiDocs {

    private final PersonalUserService personalUserService;
    private final FileService fileService;

    @GetMapping("/me")
    public ResponseEntity<FetchUserResponse> fetchPersonalUser(@AuthenticationPrincipal Jwt jwt) {
        String personalUserId = jwt.getSubject();
        log.info("기본 정보 조회: user-{}", personalUserId);
        
        return ResponseEntity.ok(personalUserService.fetchUserById(personalUserId));
    }

    @GetMapping("/me/info")
    public ResponseEntity<PersonalUserResponse> getMyInfo(@AuthenticationPrincipal Jwt jwt) {
        String personalUserId = jwt.getSubject();
        log.info("나의 정보 조회: user-{}", personalUserId);
        
        return ResponseEntity.ok(personalUserService.getUserById(personalUserId));
    }

    @PutMapping("/me/info")
    public ResponseEntity<PersonalUserResponse> updateMyInfo(@AuthenticationPrincipal Jwt jwt, @RequestBody UpdatePersonalUserRequest request) {
        String personalUserId = jwt.getSubject();
        log.info("나의 정보 수정 : user-{}", personalUserId);
        return ResponseEntity.ok(personalUserService.updateUserById(personalUserId, request));
    }   

    @GetMapping("/me/carrots")
    public ResponseEntity<CarrotsResponse> getMyCarrots(@AuthenticationPrincipal Jwt jwt) {
        String personalUserId = jwt.getSubject();
        log.info("보유 캐럿 조회: user-{}", personalUserId);

        return ResponseEntity.ok(personalUserService.getCarrotsById(personalUserId));
    }   

    @GetMapping("/me/hold-bunnies")
    public ResponseEntity<HoldBunniesResponse> getMyHoldBunnies(@AuthenticationPrincipal Jwt jwt) {
        String personalUserId = jwt.getSubject();
        log.info("보유 버니 조회: user-{}", personalUserId);

        return ResponseEntity.ok(personalUserService.getBunniesById(personalUserId));
    }

    @GetMapping("/me/hold-bunnies/stats")
    public ResponseEntity<HoldBunniesStatsResponse> getMyHoldBunniesStats(@AuthenticationPrincipal Jwt jwt) {
        String personalUserId = jwt.getSubject();
        log.info("보유 버니 통계 조회: user-{}", personalUserId);

        return ResponseEntity.ok(personalUserService.getBunniesStatsById(personalUserId));
    }

    @GetMapping("/me/orders")
    public ResponseEntity<OrderListResponse> getMyOrders(@AuthenticationPrincipal Jwt jwt) {
        String personalUserId = jwt.getSubject();
        log.info("미체결 주문 목록 조회: user-{}", personalUserId);  

        return ResponseEntity.ok(personalUserService.getOrdersById(personalUserId));
    }

    @GetMapping("/me/matches")
    public ResponseEntity<MatchListResponse> getMyMatches(@AuthenticationPrincipal Jwt jwt) {
        String personalUserId = jwt.getSubject();
        log.info("체결 주문 목록 조회: user-{}", personalUserId);  

        return ResponseEntity.ok(personalUserService.getMatchesById(personalUserId));
    }

    @PostMapping("/me/upload")
    public ResponseEntity<String> upload(@RequestParam("file") MultipartFile file, @AuthenticationPrincipal Jwt jwt) {
        String personalUserId = jwt.getSubject();
        log.info("파일 업로드 요청: user-{}", personalUserId); 
        String url = fileService.uploadFile(file, personalUserId);
        return ResponseEntity.ok(url); 
    }
}