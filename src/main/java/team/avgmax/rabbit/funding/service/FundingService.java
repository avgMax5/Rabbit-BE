package team.avgmax.rabbit.funding.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import team.avgmax.rabbit.funding.dto.request.CreateFundBunnyRequest;
import team.avgmax.rabbit.funding.dto.request.CreateFundingRequest;
import team.avgmax.rabbit.funding.dto.response.FundBunnyDetailResponse;
import team.avgmax.rabbit.funding.dto.response.FundBunnyCountResponse;
import team.avgmax.rabbit.funding.dto.response.FundBunnyListResponse;
import team.avgmax.rabbit.funding.dto.response.FundBunnyResponse;
import team.avgmax.rabbit.funding.dto.response.FundingResponse;
import team.avgmax.rabbit.funding.dto.data.UserFundingSummary;
import team.avgmax.rabbit.funding.controller.enums.FundBunnySortType;
import team.avgmax.rabbit.funding.entity.FundBunny;
import team.avgmax.rabbit.funding.entity.Funding;
import team.avgmax.rabbit.funding.exception.FundingError;
import team.avgmax.rabbit.funding.exception.FundingException;
import team.avgmax.rabbit.bunny.repository.BunnyRepository;
import team.avgmax.rabbit.ai.service.ChatClientService;
import team.avgmax.rabbit.funding.repository.FundBunnyRepository;
import team.avgmax.rabbit.funding.repository.FundingRepository;
import team.avgmax.rabbit.user.repository.HoldBunnyRepository;
import team.avgmax.rabbit.user.service.PersonalUserService;
import team.avgmax.rabbit.user.entity.HoldBunny;
import team.avgmax.rabbit.user.entity.PersonalUser;
import team.avgmax.rabbit.bunny.entity.Bunny;
import team.avgmax.rabbit.bunny.entity.enums.BunnyType;
import team.avgmax.rabbit.global.util.RedisUtil;

import java.util.regex.Pattern;

import java.util.Arrays;
import java.util.List;
import java.math.BigDecimal;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class FundingService {
    private final PersonalUserService personalUserService;
    private final ChatClientService chatClientService;

    private final FundingRepository fundingRepository;
    private final FundBunnyRepository fundBunnyRepository;
    private final BunnyRepository bunnyRepository;
    private final HoldBunnyRepository holdBunnyRepository;
    private final RedisUtil redisUtil;

    @Value("${app.redis.fund-bunny.expiry}")
    private Long fundBunnyExpiry;

    @Transactional(readOnly = true)
    public FundBunnyCountResponse getFundBunnyCount() {
        return FundBunnyCountResponse.of(bunnyRepository.count(), fundBunnyRepository.count(), fundBunnyRepository.countByEndAtWithin24Hours());
    }

    @Transactional
    public FundBunnyResponse createFundBunny(CreateFundBunnyRequest request, String userId) {
        PersonalUser user = personalUserService.findPersonalUserById(userId);
        validateAlreadyFundBunnyUser(user);
        validateBunnyName(request.bunnyName());
        validateBunnyType(request.bunnyType());
        FundBunny fundBunny = fundBunnyRepository.save(FundBunny.create(request, user, fundBunnyExpiry));
        
        redisUtil.setData("fund_bunny:" + fundBunny.getId(), "expiration_marker", fundBunnyExpiry);
        
        return FundBunnyResponse.from(fundBunny);
    }

    @Transactional(readOnly = true)
    public boolean checkDuplicateBunnyName(String bunnyName) {
        return fundBunnyRepository.existsByBunnyName(bunnyName) || bunnyRepository.existsByBunnyName(bunnyName);
    }

    @Transactional(readOnly = true)
    public FundBunnyListResponse getFundBunnyList(FundBunnySortType sortType, Pageable pageable) {
        Page<FundBunny> fundBunnies = switch (sortType) {
            case OLDEST -> fundBunnyRepository.findAllByOrderByCreatedAtAsc(pageable);
            case NEWEST -> fundBunnyRepository.findAllByOrderByCreatedAtDesc(pageable);
            case MOST_INVESTED -> fundBunnyRepository.findAllByOrderByCollectedAmountDesc(pageable);
            case LEAST_INVESTED -> fundBunnyRepository.findAllByOrderByCollectedAmountAsc(pageable);
        };
        List<FundBunnyResponse> fundBunniesResponse = FundBunnyResponse.from(fundBunnies.getContent());
        return FundBunnyListResponse.from(fundBunniesResponse);
    }

    @Transactional(readOnly = true)
    public FundBunnyDetailResponse getFundBunnyDetail(String fundBunnyId, String userId) {
        PersonalUser user = personalUserService.findPersonalUserById(userId);
        FundBunny fundBunny = findFundBunnyById(fundBunnyId);
        
        List<UserFundingSummary> userFundingSummaries = fundingRepository.findUserFundingSummariesByFundBunnyOrderByQuantityDesc(fundBunny);
        BigDecimal myHoldingQuantity = fundingRepository.findTotalQuantityByUserAndFundBunny(user, fundBunny);
        
        return FundBunnyDetailResponse.of(fundBunny, user, userFundingSummaries, myHoldingQuantity);
    }

    @Transactional
    public Optional<FundingResponse> createFunding(String fundBunnyId, String userId, CreateFundingRequest request) {
        PersonalUser user = personalUserService.findPersonalUserById(userId);
        FundBunny fundBunny = findFundBunnyById(fundBunnyId);
        BigDecimal myHoldingQuantity = fundingRepository.findTotalQuantityByUserAndFundBunny(user, fundBunny);

        validateBnyQuantity(fundBunny, myHoldingQuantity, request.fundBny());

        Funding funding = Funding.create(fundBunny, user, request);
        user.subtractCarrot(request.fundBny().multiply(fundBunny.getType().getPrice()));
        fundBunny.addBny(request.fundBny());
        fundingRepository.save(funding);
        
        // 상장 조건 확인 및 처리
        if (fundBunny.isReadyForListing()) {
            processListing(fundBunny);
            return Optional.empty();
        }
        
        return Optional.of(FundingResponse.from(funding, myHoldingQuantity));
    }

    private FundBunny findFundBunnyById(String fundBunnyId) {
        return fundBunnyRepository.findById(fundBunnyId)
                .orElseThrow(() -> new FundingException(FundingError.FUND_BUNNY_NOT_FOUND));
    }

    private void validateAlreadyFundBunnyUser(PersonalUser user) {
        if (fundBunnyRepository.existsByUser(user)) {
            throw new FundingException(FundingError.ALREADY_FUND_BUNNY_USER);
        }
    }

    private void validateBunnyName(String bunnyName) {
        if (bunnyName == null || bunnyName.trim().isEmpty()) {
            throw new FundingException(FundingError.BUNNY_NAME_REQUIRED);
        }
        if (bunnyName.length() < 3 || bunnyName.length() > 20) {
            throw new FundingException(FundingError.BUNNY_NAME_INVALID_LENGTH);
        }
        if (bunnyName.startsWith("-") || bunnyName.endsWith("-")) {
            throw new FundingException(FundingError.BUNNY_NAME_INVALID_HYPHEN_START_END);
        }
        if (bunnyName.contains("--")) {
            throw new FundingException(FundingError.BUNNY_NAME_INVALID_CONSECUTIVE_HYPHEN);
        }
        if (!Pattern.compile("^[a-z0-9]([a-z0-9-]*[a-z0-9])?$").matcher(bunnyName).matches()) {
            throw new FundingException(FundingError.BUNNY_NAME_INVALID_CHARACTER);
        }
        if (checkDuplicateBunnyName(bunnyName)) {
            throw new FundingException(FundingError.BUNNY_NAME_DUPLICATE);
        }
    }
    
    private void validateBunnyType(BunnyType bunnyType) {
        if (bunnyType == null) {
            throw new FundingException(FundingError.BUNNY_TYPE_REQUIRED);
        }
        if (!Arrays.asList(BunnyType.values()).contains(bunnyType)) {
            throw new FundingException(FundingError.BUNNY_TYPE_INVALID);
        }
    }

    private void validateBnyQuantity(FundBunny fundBunny, BigDecimal myHoldingQuantity, BigDecimal quantity) {
        if (quantity.compareTo(BigDecimal.ZERO) <= 0) {
            throw new FundingException(FundingError.BNY_NOT_POSITIVE);
        }
        if (quantity.compareTo(fundBunny.getType().getTotalSupply().subtract(fundBunny.getCollectedBny())) > 0) {
            throw new FundingException(FundingError.BNY_OVER_REMAINING);
        }
        if (myHoldingQuantity.add(quantity).compareTo(fundBunny.getType().getTotalSupply().multiply(new BigDecimal("0.5"))) > 0) {
            throw new FundingException(FundingError.BNY_OVER_50);
        }
    }

    private void processListing(FundBunny fundBunny) {
        // 1. 상장한 User의 Role을 BUNNY로 변경
        fundBunny.getUser().updateRoleToBunny();

        // 2. FundBunny를 Bunny로 변환하여 저장
        Bunny bunny = bunnyRepository.save(fundBunny.convertToBunny());

        // 3. 해당 FundBunny의 모든 Funding 조회
        List<Funding> fundings = fundingRepository.findByFundBunny(fundBunny);
        
        // 4. 조회한 Funding들로 HoldBunny들을 생성하여 저장
        List<HoldBunny> holdBunnies = fundBunny.createHoldBunnies(bunny, fundings);
        holdBunnyRepository.saveAll(holdBunnies);

        // 5. AI Review와 AI Feedback 생성
        String aiReview = chatClientService.getAiReviewOfBunny(bunny);
        String aiFeedback = chatClientService.getAiFeedbackOfBunny(bunny);
        bunny.updateAiReviewAndFeedback(aiReview, aiFeedback);

        // 6. Redis에서 만료 키 제거 (상장되면 만료 처리할 필요 없음)
        redisUtil.deleteData("fund_bunny:" + fundBunny.getId());

        // 7. FundBunny 삭제 (CASCADE로 Funding도 함께 삭제)
        fundBunnyRepository.delete(fundBunny);
    }

    @Transactional
    public void processFundBunnyExpiration(String fundBunnyId) {
        FundBunny fundBunny = fundBunnyRepository.findById(fundBunnyId)
                .orElseThrow(() -> new FundingException(FundingError.FUND_BUNNY_NOT_FOUND));

        // 각 Funding에 대해 환불 처리
        List<Funding> fundings = fundingRepository.findByFundBunny(fundBunny);
        for (Funding funding : fundings) {
            PersonalUser user = funding.getUser();
            BigDecimal refundAmount = funding.getQuantity().multiply(fundBunny.getType().getPrice());
            user.addCarrot(refundAmount);
        }

        // FundBunny 삭제 (CASCADE로 Funding도 함께 삭제)
        fundBunnyRepository.delete(fundBunny);
    }
}
