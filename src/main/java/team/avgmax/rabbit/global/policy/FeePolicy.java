package team.avgmax.rabbit.global.policy;

import java.math.BigDecimal;
import java.math.RoundingMode;

public final class FeePolicy {

    private FeePolicy() {}

    // 수수료율 (0.1%)
    public static final BigDecimal FEE_RATE = new BigDecimal("0.001");

    // 모든 금액 결과를 원 단위로 반올림
    private static final int SCALE = 0;
    private static final RoundingMode ROUNDING = RoundingMode.HALF_UP;

    // 수수료 계산
    public static BigDecimal calcFee(BigDecimal tradeAmount) {
        if (tradeAmount == null || tradeAmount.signum() <= 0) {
            return BigDecimal.ZERO.setScale(SCALE, ROUNDING);
        }

        return tradeAmount.multiply(FEE_RATE).setScale(SCALE, ROUNDING);
    }

    // 매수/매도 양쪽 동일 수수료 반환
    public static FeePair calcBothSide(BigDecimal tradeAmount) {
        BigDecimal fee = calcFee(tradeAmount);
        return new FeePair(fee, fee);
    }

    // 최종 금액 정규화 (scale/rounding 일치)
    public static BigDecimal normalize(BigDecimal amount) {
        if (amount == null) return BigDecimal.ZERO.setScale(SCALE, ROUNDING);
        return amount.setScale(SCALE, ROUNDING);
    }
}