package team.avgmax.rabbit.bunny.service.match;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import team.avgmax.rabbit.bunny.entity.Bunny;
import team.avgmax.rabbit.bunny.entity.Match;
import team.avgmax.rabbit.bunny.entity.Order;
import team.avgmax.rabbit.bunny.entity.enums.OrderType;
import team.avgmax.rabbit.bunny.repository.MatchRepository;
import team.avgmax.rabbit.bunny.repository.OrderRepository;
import team.avgmax.rabbit.global.money.MoneyCalc;
import team.avgmax.rabbit.user.entity.PersonalUser;
import team.avgmax.rabbit.user.repository.HoldBunnyRepository;
import team.avgmax.rabbit.user.repository.PersonalUserRepository;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Slf4j
@Component
@RequiredArgsConstructor
public class MatchingEngine {

    private final MatchRepository matchRepository;
    private final OrderRepository orderRepository;
    private final HoldBunnyRepository holdBunnyRepository;
    private final PersonalUserRepository personalUserRepository;

    @Transactional(propagation = Propagation.MANDATORY)
    public MatchingResult match(Bunny bunny, Order myOrder, List<Order> candidates) {
        final Set<BigDecimal> touchedBid = new HashSet<>();
        final Set<BigDecimal> touchedAsk = new HashSet<>();

        BigDecimal remainingQty = myOrder.getQuantity();

        for (Order counter : candidates) {
            if (remainingQty.signum() <= 0) break;
            if (counter.getQuantity().signum() <= 0) continue;

            // 거래 가능 수량
            BigDecimal tradable = remainingQty.min(counter.getQuantity());
            if (tradable.signum() <= 0) continue;

            BigDecimal tradePrice   = counter.getUnitPrice();                     // 체결가
            BigDecimal tradeBaseAmt = MoneyCalc.baseAmount(tradable, tradePrice); // 원금(원)

            // 체결 기록
            Match match = Match.builder()
                    .bunny(bunny)
                    .buyUser(myOrder.getOrderType() == OrderType.BUY ? myOrder.getUser() : counter.getUser())
                    .sellUser(myOrder.getOrderType() == OrderType.SELL ? myOrder.getUser() : counter.getUser())
                    .quantity(tradable)
                    .unitPrice(tradePrice)
                    .build();
            matchRepository.save(match);

            // 현재가 업데이트 (가장 최신 체결가)
            bunny.updateCurrentPrice(tradePrice);

            // 정산 (보유 / 캐럿)
            PersonalUser buyer  = match.getBuyUser();
            PersonalUser seller = match.getSellUser();

            // 락 (동일 트랜잭션에서 write 충돌 예방)
            personalUserRepository.findByIdForUpdate(buyer.getId());
            personalUserRepository.findByIdForUpdate(seller.getId());

            // 매도자 수입 (원금 - 수수료)
            BigDecimal sellerIncome = MoneyCalc.sellerIncome(tradeBaseAmt);
            personalUserRepository.addCarrotForUpdate(seller.getId(), sellerIncome);

            // 매수자 예약금-실지출 차액 환불
            if (myOrder.getOrderType() == OrderType.BUY) {
                BigDecimal refund = MoneyCalc.buyerRefundForPriceImprovement(myOrder.getUnitPrice(), tradePrice, tradable);
                if (refund.signum() > 0) {
                    personalUserRepository.addCarrotForUpdate(buyer.getId(), refund);
                }
            }

            // 보유 변동
            holdBunnyRepository.applyBuyMatch(buyer.getId(), bunny.getId(), tradable, tradeBaseAmt);
            holdBunnyRepository.applySellMatch(seller.getId(), bunny.getId(), tradeBaseAmt);

            // 주문 잔량 갱신
            remainingQty = remainingQty.subtract(tradable);
            counter.decreaseQuantity(tradable);

            // 상대 주문이 0이면 제거
            if (counter.getQuantity().signum() == 0) {
                orderRepository.delete(counter);
                if (counter.getOrderType() == OrderType.SELL) {
                    holdBunnyRepository.deleteIfEmpty(counter.getUser().getId(), bunny.getId());
                }
            }

            // touched 가격 기록 (상대편 가격)
            if (myOrder.getOrderType() == OrderType.BUY) {
                touchedAsk.add(counter.getUnitPrice());
            } else {
                touchedBid.add(counter.getUnitPrice());
            }
        }

        // myOrder 잔량 처리
        if (remainingQty.signum() == 0) {
            orderRepository.delete(myOrder);     // 완전 체결 → 삭제
            if (myOrder.getOrderType() == OrderType.SELL) {
                holdBunnyRepository.deleteIfEmpty(myOrder.getUser().getId(), bunny.getId());
            }
        } else {
            myOrder.updateQuantity(remainingQty); // 부분 체결 → 잔량 저장
        }

        return new MatchingResult(touchedBid, touchedAsk);
    }
}
