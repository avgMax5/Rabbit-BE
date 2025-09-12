package team.avgmax.rabbit.funding.dto.response;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.Builder;

import team.avgmax.rabbit.funding.dto.data.UserFundingSummary;
import team.avgmax.rabbit.funding.entity.FundBunny;

@Builder
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record HoldingStatusResponse(
        Double top1,
        Double top2,
        Double top3,
        Double others,
        Double remaining
) {
    
    public static HoldingStatusResponse from(List<UserFundingSummary> userFundingSummaries, FundBunny fundBunny) {
        BigDecimal totalSupply = fundBunny.getType().getTotalSupply();
        
        if (userFundingSummaries.isEmpty() || totalSupply.compareTo(BigDecimal.ZERO) == 0) {
            return HoldingStatusResponse.builder()
                    .top1(0.0)
                    .top2(0.0)
                    .top3(0.0)
                    .others(0.0)
                    .remaining(100.0)
                    .build();
        }
        
        // 상위 3명의 비율 계산 (전체 공급량 대비)
        Double top1 = !userFundingSummaries.isEmpty() ?
                userFundingSummaries.get(0).totalQuantity().divide(totalSupply, 4, RoundingMode.HALF_UP).multiply(new BigDecimal("100")).doubleValue() : 0.0;
        Double top2 = userFundingSummaries.size() >= 2 ? 
                userFundingSummaries.get(1).totalQuantity().divide(totalSupply, 4, RoundingMode.HALF_UP).multiply(new BigDecimal("100")).doubleValue() : 0.0;
        Double top3 = userFundingSummaries.size() >= 3 ? 
                userFundingSummaries.get(2).totalQuantity().divide(totalSupply, 4, RoundingMode.HALF_UP).multiply(new BigDecimal("100")).doubleValue() : 0.0;
        
        // 나머지 사용자들의 비율 계산 (전체 공급량 대비)
        BigDecimal othersQuantity = BigDecimal.ZERO;
        if (userFundingSummaries.size() > 3) {
            for (int i = 3; i < userFundingSummaries.size(); i++) {
                othersQuantity = othersQuantity.add(userFundingSummaries.get(i).totalQuantity());
            }
        }
        Double others = othersQuantity.divide(totalSupply, 4, RoundingMode.HALF_UP).multiply(new BigDecimal("100")).doubleValue();
        
        // 남은 비율 계산 (아직 펀딩되지 않은 부분)
        Double remaining = 100.0 - top1 - top2 - top3 - others;

        return HoldingStatusResponse.builder()
                .top1(top1)
                .top2(top2)
                .top3(top3)
                .others(others)
                .remaining(remaining)
                .build();
    }
}
