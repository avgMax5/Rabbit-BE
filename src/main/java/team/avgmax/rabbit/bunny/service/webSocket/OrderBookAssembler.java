package team.avgmax.rabbit.bunny.service.webSocket;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import team.avgmax.rabbit.bunny.dto.orderBook.OrderBookLevel;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

@Component
@RequiredArgsConstructor
public class OrderBookAssembler {

    private static final int DEFAULT_MAX_LEVELS = 20;

    public List<OrderBookLevel> toLevel(List<OrderLeaf> leaves) {
        // 1) 가격 별 합산
        Map<BigDecimal, BigDecimal> levelMap = new HashMap<>();

        for (OrderLeaf leaf : leaves) {
            // 남은 수량이 없거나 음수면 호가창에 올리지 않음
            if (leaf.remainingQty() == null || leaf.remainingQty().signum() <= 0) continue;

            BigDecimal priceKey = normalizePrice(leaf.price);
            if (priceKey == null) continue;

            // 수량이 있으면 누적
            levelMap.merge(priceKey, leaf.remainingQty(), BigDecimal::add);
        }


        // 2) 불변 DTO 로 변환 및 정렬 및 상위 N개 슬라이싱 후 return
        return levelMap.entrySet().stream()
                .filter(e -> e.getValue() != null && e.getValue().signum() > 0)
                .map(e -> new OrderBookLevel(e.getKey(), e.getValue()))
                .sorted(Comparator.comparing(OrderBookLevel::price).reversed())
                .limit(DEFAULT_MAX_LEVELS)
                .toList();
    }

    public record OrderLeaf(
            BigDecimal price,
            BigDecimal remainingQty
    ) {}

    public static BigDecimal normalizePrice(BigDecimal price) {
        if (price == null) return null;
        return price.setScale(0, RoundingMode.UNNECESSARY).stripTrailingZeros();
    }
}
