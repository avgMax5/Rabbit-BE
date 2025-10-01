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
import team.avgmax.rabbit.bunny.service.BunnyIndicatorService;
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

    private final BunnyIndicatorService bunnyIndicatorService;

    @Transactional(propagation = Propagation.MANDATORY)
    public MatchingResult match(Bunny bunny, Order myOrder, List<Order> candidates) {
        final Set<BigDecimal> touchedBid = new HashSet<>();
        final Set<BigDecimal> touchedAsk = new HashSet<>();
        final Set<String> affectedUsers = new HashSet<>();

        for (Order counter : candidates) {
            if (myOrder.getQuantity().signum() <= 0) break;  // myOrder 엔티티에서 직접 확인
            if (counter.getQuantity().signum() <= 0) continue;

            // 거래 가능 수량
            BigDecimal tradable = myOrder.getQuantity().min(counter.getQuantity());
            log.info("tradable: {}", tradable);
            if (tradable.signum() <= 0) continue;

            BigDecimal tradePrice   = counter.getUnitPrice();                     // 체결가
            BigDecimal tradeBaseAmt = MoneyCalc.baseAmount(tradable, tradePrice); // 원금(원)

            // 체결 기록
            Match match = matchRepository.save(Match.create(bunny, myOrder, counter, tradable, tradePrice));

            // 현재가 업데이트 (가장 최신 체결가)
            bunny.updateCurrentPrice(tradePrice);

            // 정산 (보유 / 캐럿)
            // 락 (동일 트랜잭션에서 write 충돌 예방)
            PersonalUser buyer  = personalUserRepository.findByIdForUpdate(match.getBuyUser().getId());
            PersonalUser seller = personalUserRepository.findByIdForUpdate(match.getSellUser().getId());

            // 매도자 수입 (원금 - 수수료)
            BigDecimal sellerIncome = MoneyCalc.sellerIncome(tradeBaseAmt);
            seller.addCarrot(sellerIncome);

            // 매수자 예약금-실지출 차액 환불
            if (myOrder.getOrderType() == OrderType.BUY) {
                BigDecimal refund = MoneyCalc.buyerRefundForPriceImprovement(tradable, tradePrice, myOrder.getUnitPrice());
                if (refund.signum() > 0) {
                    buyer.addCarrot(refund);
                }
            }
            // 보유 변동 (원자적 UPDATE 쿼리 - 동시성 안전)
            // 매수자: holdQuantity 증가 + costBasis 증가
            holdBunnyRepository.applyBuyMatch(buyer.getId(), bunny.getId(), tradable, tradeBaseAmt);
            // 매도자: costBasis만 감소 (holdQuantity는 주문 생성 시 이미 선차감됨)
            holdBunnyRepository.applySellMatch(seller.getId(), bunny.getId(), tradeBaseAmt);

            // 영향받은 매도자 추적
            affectedUsers.add(seller.getId());

            // 주문 잔량 갱신

            myOrder.decreaseQuantity(tradable);
            counter.decreaseQuantity(tradable);

            log.info("counter.getQuantity(): {}", counter.getQuantity());

            // 상대 주문 잔량 처리
            if (counter.getQuantity().signum() <= 0) {
                orderRepository.delete(counter);     // 완전 체결 → 삭제
            } else {
                orderRepository.save(counter);       // 부분 체결 → 잔량 명시적 저장
            }

            // touched 가격 기록 (상대편 가격)
            if (myOrder.getOrderType() == OrderType.BUY) {
                touchedAsk.add(counter.getUnitPrice());
            } else {
                touchedBid.add(counter.getUnitPrice());
            }
        }

        log.info("myOrder.getQuantity(): {}", myOrder.getQuantity());

        // myOrder 잔량 처리
        if (myOrder.getQuantity().signum() <= 0) {
            orderRepository.delete(myOrder);     // 완전 체결 → 삭제
            if (myOrder.getOrderType() == OrderType.SELL) {
                affectedUsers.add(myOrder.getUser().getId());
            }
        } else {
            // 부분 체결 → 잔량 명시적 저장 (JPA dirty checking 보장)
            orderRepository.save(myOrder);
        }

        // 영향받은 모든 사용자의 HoldBunny 정리 (수량 0이고 열린 SELL 주문이 없으면 삭제)
        for (String userId : affectedUsers) {
            holdBunnyRepository.deleteIfEmpty(userId, bunny.getId());
        }

        bunnyIndicatorService.updateBunnyValue(bunny);

        return new MatchingResult(touchedBid, touchedAsk, bunny.getCurrentPrice());
    }
}
