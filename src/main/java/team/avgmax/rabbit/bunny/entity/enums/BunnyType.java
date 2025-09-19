package team.avgmax.rabbit.bunny.entity.enums;

import java.math.BigDecimal;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum BunnyType {
    A(
        "희소 자산형",
        BigDecimal.valueOf(100_000_000),
        BigDecimal.valueOf(1_000),
        BigDecimal.valueOf(100_000)
    ),
    B(
        "밸런스형",
        BigDecimal.valueOf(100_000_000),
        BigDecimal.valueOf(100_000),
        BigDecimal.valueOf(1_000)
    ),
    C(
        "단가 친화형",
        BigDecimal.valueOf(100_000_000),
        BigDecimal.valueOf(1_000_000),
        BigDecimal.valueOf(100)
    );

    private final String displayName;
    private final BigDecimal marketCap;  // 시총 (C)
    private final BigDecimal totalSupply; // 개수 (BNY)
    private final BigDecimal price;      // 단가 (C)
}
