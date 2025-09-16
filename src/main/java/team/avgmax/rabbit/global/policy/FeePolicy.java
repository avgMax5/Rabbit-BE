package team.avgmax.rabbit.global.policy;

import java.math.BigDecimal;
import java.math.RoundingMode;

public final class FeePolicy {

    private FeePolicy() {}

    public static final BigDecimal FEE_RATE = new BigDecimal("0.001");

    public static BigDecimal calcFee(BigDecimal tradeAmount) {
        if (tradeAmount == null) return null;
        return tradeAmount.multiply(FEE_RATE).setScale(0, RoundingMode.HALF_UP);
    }

    public static FeePair calcBothSides(BigDecimal tradeAmount) {
        BigDecimal fee = calcFee(tradeAmount);
        return new FeePair(fee, fee);
    }
}