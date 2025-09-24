package team.avgmax.rabbit.user.dto.response;

import java.math.BigDecimal;
import java.math.RoundingMode;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import lombok.Builder;
import team.avgmax.rabbit.user.entity.HoldBunny;

@Builder
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record HoldBunnyResponse(
    String bunnyId,
    String bunnyName,
    BigDecimal holdQuantity,    // 보유량
    BigDecimal totalBuyAmount,  // 매입금액
    BigDecimal valuation,       // 평가금액
    BigDecimal avgPrice,        // 평균단가
    BigDecimal profitOrLoss,    // 평가손익
    BigDecimal returnRate,      // 수익률
    BigDecimal currentPrice,    // 현재가
    BigDecimal priceDiffFromYesterday, // 전일비
    BigDecimal priceChangeRate  // 등락률
) {
    public static HoldBunnyResponse from(HoldBunny holdBunny) {
        BigDecimal currentPrice = holdBunny.getBunny().getCurrentPrice(); //Bunny 엔티티가 보유한 가장 최신 거래가격
        BigDecimal closingPrice = holdBunny.getBunny().getClosingPrice(); //Bunny 엔티티가 보유한 가장 최근 종가
        BigDecimal valuation = holdBunny.getHoldQuantity().multiply(currentPrice); // 현재가 × 보유수량 = 지금 가지고 있는 자산의 시가 평가 총액
        BigDecimal avgPrice = holdBunny.getCostBasis().divide(
                holdBunny.getHoldQuantity(), RoundingMode.HALF_UP); // 	총 매입금액 / 총 보유수량 = 버니 하나를 얼마에 샀는지에 대한 평균
        BigDecimal profitOrLoss = valuation.subtract(holdBunny.getCostBasis()); // 내 수익금
        BigDecimal returnRate = profitOrLoss.divide(
                holdBunny.getCostBasis(), RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100)); // 수익률 = (평가손익 / 총 매입금액) * 100
        BigDecimal priceDiffFromYesterday = currentPrice.subtract(closingPrice); // 등락가 = 현재가 - 전일 종가
        BigDecimal priceChangeRate = priceDiffFromYesterday.divide(
                closingPrice, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100)); // 등락률 = (전일비 / 전일 종가) * 100

        return HoldBunnyResponse.builder()
            .bunnyId(holdBunny.getBunny().getId())
            .bunnyName(holdBunny.getBunny().getBunnyName())
            .holdQuantity(holdBunny.getHoldQuantity())
            .totalBuyAmount(holdBunny.getCostBasis())
            .valuation(valuation)
            .avgPrice(avgPrice)
            .profitOrLoss(profitOrLoss)
            .returnRate(returnRate)
            .currentPrice(currentPrice)
            .priceDiffFromYesterday(priceDiffFromYesterday)
            .priceChangeRate(priceChangeRate)
            .build();
    }
}