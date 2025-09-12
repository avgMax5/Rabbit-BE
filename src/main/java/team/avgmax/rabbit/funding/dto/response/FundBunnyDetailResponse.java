package team.avgmax.rabbit.funding.dto.response;

import java.math.BigDecimal;
import java.math.RoundingMode;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.Builder;
import team.avgmax.rabbit.user.dto.response.SpecResponse;
import team.avgmax.rabbit.funding.entity.FundBunny;
import team.avgmax.rabbit.user.entity.PersonalUser;
import team.avgmax.rabbit.funding.dto.data.UserFundingSummary;

import java.util.List;

@Builder
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record FundBunnyDetailResponse(
        String fundBunnyId,
        String bunnyName,
        String bunnyType,
        BigDecimal targetBny,
        BigDecimal collectedBny,
        BigDecimal availableBny,
        BigDecimal myHoldingQuantity,
        HoldingStatusResponse holdingStatus,
        BigDecimal myAccountBny,
        BigDecimal myAccountC,
        SpecResponse spec
    ) {
    public static FundBunnyDetailResponse of(FundBunny fundBunny, PersonalUser user, List<UserFundingSummary> userFundingSummaries, BigDecimal myHoldingQuantity) {
        // 1. 사용자 계좌 잔액
        BigDecimal myAccountC = user.getCarrot();
        BigDecimal myAccountBny = myAccountC.divide(fundBunny.getType().getPrice(), RoundingMode.DOWN);
        
        // 2. 목표까지 남은 금액
        BigDecimal remainingTargetBny = fundBunny.getType().getTotalSupply().subtract(fundBunny.getCollectedBny());
        
        // 3. 총 공급량의 50% - 나의 보유 지분
        BigDecimal maxSinglePurchaseBny = fundBunny.getType().getTotalSupply().multiply(new BigDecimal("0.5")).subtract(myHoldingQuantity);
        
        // availableBny는 위 세 값 중 최소값
        BigDecimal availableBny = myAccountBny
                .min(remainingTargetBny)
                .min(maxSinglePurchaseBny);
        
        return FundBunnyDetailResponse.builder()
                .fundBunnyId(fundBunny.getId())
                .bunnyName(fundBunny.getBunnyName())
                .bunnyType(fundBunny.getType().name())
                .targetBny(fundBunny.getType().getTotalSupply())
                .collectedBny(fundBunny.getCollectedBny())
                .availableBny(availableBny)
                .myHoldingQuantity(myHoldingQuantity)
                .holdingStatus(HoldingStatusResponse.from(userFundingSummaries, fundBunny))
                .myAccountBny(myAccountBny)
                .myAccountC(myAccountC)
                .spec(SpecResponse.from(fundBunny.getUser()))
                .build();
    }
}
