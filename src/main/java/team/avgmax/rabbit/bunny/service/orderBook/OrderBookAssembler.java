package team.avgmax.rabbit.bunny.service.orderBook;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import team.avgmax.rabbit.bunny.dto.orderBook.OrderBookLevel;
import team.avgmax.rabbit.bunny.entity.enums.OrderType;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

@Component
@RequiredArgsConstructor
public class OrderBookAssembler {

    private static final int DEFAULT_MAX_LEVELS = 20;

    public List<OrderBookLevel> toLevel(List<OrderLeaf> leaves) {
        // 1) 매수와 매도로 분리하여 가격별 합산
        Map<BigDecimal, BigDecimal> buyLevelMap = new HashMap<>();
        Map<BigDecimal, BigDecimal> sellLevelMap = new HashMap<>();

        for (OrderLeaf leaf : leaves) {
            // 남은 수량이 없거나 음수면 호가창에 올리지 않음
            if (leaf.remainingQty() == null || leaf.remainingQty().signum() <= 0) continue;

            BigDecimal priceKey = normalizePrice(leaf.price);
            if (priceKey == null) continue;

            // 매수/매도별로 수량 누적
            if (leaf.orderType() == OrderType.BUY) {
                buyLevelMap.merge(priceKey, leaf.remainingQty(), BigDecimal::add);
            } else {
                sellLevelMap.merge(priceKey, leaf.remainingQty(), BigDecimal::add);
            }
        }

        // 2) 매수 레벨 정렬 (높은 가격부터), 매도 레벨 정렬 (낮은 가격부터)
        List<OrderBookLevel> buyLevels = buyLevelMap.entrySet().stream()
                .filter(e -> e.getValue() != null && e.getValue().signum() > 0)
                .map(e -> new OrderBookLevel(e.getKey(), e.getValue(), OrderType.BUY))
                .sorted(Comparator.comparing(OrderBookLevel::price).reversed()) // 높은 가격부터
                .toList();

        List<OrderBookLevel> sellLevels = sellLevelMap.entrySet().stream()
                .filter(e -> e.getValue() != null && e.getValue().signum() > 0)
                .map(e -> new OrderBookLevel(e.getKey(), e.getValue(), OrderType.SELL))
                .sorted(Comparator.comparing(OrderBookLevel::price)) // 낮은 가격부터
                .toList();

        // 3) 전체 한도(DEFAULT_MAX_LEVELS) 내에서 매수/매도 비율 조정
        return combineBuyAndSellLevels(buyLevels, sellLevels);
    }

    private List<OrderBookLevel> combineBuyAndSellLevels(List<OrderBookLevel> buyLevels, List<OrderBookLevel> sellLevels) {
        List<OrderBookLevel> result = new ArrayList<>();
        List<OrderBookLevel> buyList = new ArrayList<>(buyLevels);
        List<OrderBookLevel> sellList = new ArrayList<>(sellLevels);

        // 각각 최대 10개씩 추가
        int buyCount = Math.min(buyList.size(), 10);
        int sellCount = Math.min(sellList.size(), 10);
        
        // 부족한 한쪽에 대해 다른쪽에서 보충
        if (buyCount < 10 && sellList.size() > 10) {
            sellCount = Math.min(10 + (10 - buyCount), sellList.size());
        } else if (sellCount < 10 && buyList.size() > 10) {
            buyCount = Math.min(10 + (10 - sellCount), buyList.size());
        }

        // 결과 조합
        result.addAll(buyList.subList(0, buyCount));
        result.addAll(sellList.subList(0, sellCount));

        // 가격 기준 내림차순 정렬
        result.sort(Comparator.comparing(OrderBookLevel::price).reversed());

        return result;
    }

    public record OrderLeaf(
            BigDecimal price,
            BigDecimal remainingQty,
            OrderType orderType
    ) {}

    public static BigDecimal normalizePrice(BigDecimal price) {
        if (price == null) return null;
        return price.setScale(0, RoundingMode.UNNECESSARY).stripTrailingZeros();
    }
}
