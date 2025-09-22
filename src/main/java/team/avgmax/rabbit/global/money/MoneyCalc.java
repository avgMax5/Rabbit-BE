package team.avgmax.rabbit.global.money;

import team.avgmax.rabbit.global.policy.FeePolicy;

import java.math.BigDecimal;

public final class MoneyCalc {

    private MoneyCalc() {}

    // qty * price 를 원 단위로 계산
    public static BigDecimal baseAmount(BigDecimal qty, BigDecimal unitPrice) {
        return FeePolicy.normalize(qty.multiply(unitPrice));
    }

    // 어떤 금액 amount에 대한 수수료 (원 단위)
    public static BigDecimal feeOn(BigDecimal amount) {
        return FeePolicy.calcFee(amount);
    }

    // base + fee(base) = 예약금/총지출 상한
    public static BigDecimal grossWithFee(BigDecimal base) {
        return FeePolicy.normalize(base.add(feeOn(base)));
    }

    // (매수용) 예약금: qty * price + fee(qty*price)
    public static BigDecimal buyerReservation(BigDecimal qty, BigDecimal unitPrice) {
        return grossWithFee(baseAmount(qty, unitPrice));
    }

    // (매수 체결 시) 내 지정가 > 체결가일 때 환불해야 할 금액(원금+수수료)
    public static BigDecimal buyerRefundForPriceImprovement(BigDecimal myLimitPrice,
                                                            BigDecimal tradePrice,
                                                            BigDecimal filledQty) {
        // 환불 기준 원금 = (내지정가 - 체결가) * 체결수량
        BigDecimal diff = myLimitPrice.subtract(tradePrice);
        if (diff.signum() <= 0 || filledQty.signum() <= 0) return FeePolicy.normalize(BigDecimal.ZERO);

        BigDecimal baseRefund = baseAmount(filledQty, diff); // 원금 차액(원)
        BigDecimal feeRefund  = feeOn(baseRefund);           // 차액에 대한 수수료 환불(원)
        return FeePolicy.normalize(baseRefund.add(feeRefund));
    }

    // (매수 취소) 남은 잔여에 대한 예약금 환불(원금+수수료)
    public static BigDecimal buyerCancelRefund(BigDecimal remainingQty, BigDecimal limitPrice) {
        BigDecimal base = baseAmount(remainingQty, limitPrice);
        return grossWithFee(base);
    }

    // 안전한 0 값
    public static BigDecimal zero() {
        return FeePolicy.normalize(BigDecimal.ZERO);
    }

    // 매도자 실수령
    public static BigDecimal sellerIncome(BigDecimal tradeAmount) {
        return FeePolicy.normalize(tradeAmount.subtract(feeOn(tradeAmount)));
    }

    public static record Trade(
            BigDecimal tradeAmount,       // 체결 시 원금
            BigDecimal buyerFee,          // 매수자 수수료
            BigDecimal sellerFee,         // 매도자 수수료
            BigDecimal sellerIncome,      // 매도자 실수령액 (tradeAmount - sellerFee)
            BigDecimal buyerRefundTotal   // (지정가 > 체결가) 원금 + 수수료 환불
    ) {}

    public static Trade settleOne(BigDecimal filledQty, BigDecimal tradePrice, BigDecimal buyerLimitPrice) {
        BigDecimal tradeAmount = baseAmount(filledQty, tradePrice);
        BigDecimal buyerFee = feeOn(tradeAmount);
        BigDecimal sellerFee = feeOn(tradeAmount);
        BigDecimal income = sellerIncome(tradeAmount);
        BigDecimal refund = buyerRefundForPriceImprovement(filledQty, tradePrice, buyerLimitPrice);

        return new Trade(tradeAmount, buyerFee, sellerFee, income, refund);
    }
}
