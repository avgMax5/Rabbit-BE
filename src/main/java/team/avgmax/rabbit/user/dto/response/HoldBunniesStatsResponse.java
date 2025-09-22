package team.avgmax.rabbit.user.dto.response;

import java.util.List;
import java.util.Map;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import lombok.Builder;
import team.avgmax.rabbit.bunny.entity.enums.BunnyType;
import team.avgmax.rabbit.bunny.entity.enums.DeveloperType;
import team.avgmax.rabbit.user.entity.HoldBunny;
import team.avgmax.rabbit.user.entity.enums.Position;

@Builder
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record HoldBunniesStatsResponse(
    LocalDateTime timestamp,
    BigDecimal totalMarketCap,
    PositionStats position,
    DeveloperTypeStats developerType,
    CoinTypeStats coinType
) {
    
    private static <T extends Enum<T>> TopStats findTopField(List<HoldBunny> holdBunnies, Class<T> enumClass, Function<HoldBunny, T> enumExtractor) {
        return holdBunnies.stream()
            .collect(Collectors.groupingBy(
                enumExtractor,
                Collectors.reducing(
                    BigDecimal.ZERO,
                    holdBunny -> holdBunny.getBunny().getMarketCap(),
                    BigDecimal::add
                )
            ))
            .entrySet().stream()
            .max(Map.Entry.comparingByValue())
            .map(entry -> new TopStats(
                entry.getKey().name().toLowerCase(),
                entry.getValue()
            ))
            .orElse(new TopStats("unknown", BigDecimal.ZERO));
    }
    
    private static BigDecimal calculateMarketCapSum(List<HoldBunny> holdBunnies, Predicate<HoldBunny> filter) {
        return holdBunnies.stream()
            .filter(filter)
            .map(holdBunny -> holdBunny.getBunny().getMarketCap())
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
    
    private static BigDecimal calculateTotalMarketCap(List<HoldBunny> holdBunnies) {
        return calculateMarketCapSum(holdBunnies, holdBunny -> true);
    }
    
    private static double calculateRatio(BigDecimal part, BigDecimal total) {
        return total.compareTo(BigDecimal.ZERO) == 0 ? 0.0 : 
            part.divide(total, 4, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100)).doubleValue();
    }

    @Builder
    public static record PositionStats(
        double frontend,
        double backend,
        double fullstack,
        TopStats top
    ) {
        public static PositionStats from(List<HoldBunny> holdBunnies, BigDecimal totalMarketCap) {
            BigDecimal frontendMarketCap = calculateMarketCapSum(holdBunnies, 
                holdBunny -> holdBunny.getBunny().getUser().getPosition() == Position.FRONTEND);
            BigDecimal backendMarketCap = calculateMarketCapSum(holdBunnies, 
                holdBunny -> holdBunny.getBunny().getUser().getPosition() == Position.BACKEND);
            BigDecimal fullstackMarketCap = calculateMarketCapSum(holdBunnies, 
                holdBunny -> holdBunny.getBunny().getUser().getPosition() == Position.FULLSTACK);
            
            return PositionStats.builder()
                .frontend(calculateRatio(frontendMarketCap, totalMarketCap))
                .backend(calculateRatio(backendMarketCap, totalMarketCap))
                .fullstack(calculateRatio(fullstackMarketCap, totalMarketCap))
                .top(findTopField(holdBunnies, Position.class, holdBunny -> holdBunny.getBunny().getUser().getPosition()))
                .build();
        }
    }

    @Builder
    public static record DeveloperTypeStats(
        double basic,
        double growth,
        double stable,
        double value,
        double popular,
        double balance,
        TopStats top
    ) {
        public static DeveloperTypeStats from(List<HoldBunny> holdBunnies, BigDecimal totalMarketCap) {
            
            BigDecimal basicMarketCap = calculateMarketCapSum(holdBunnies, 
                holdBunny -> holdBunny.getBunny().getDeveloperType() == DeveloperType.BASIC);
            BigDecimal growthMarketCap = calculateMarketCapSum(holdBunnies, 
                holdBunny -> holdBunny.getBunny().getDeveloperType() == DeveloperType.GROWTH);
            BigDecimal stableMarketCap = calculateMarketCapSum(holdBunnies, 
                holdBunny -> holdBunny.getBunny().getDeveloperType() == DeveloperType.STABLE);
            BigDecimal valueMarketCap = calculateMarketCapSum(holdBunnies, 
                holdBunny -> holdBunny.getBunny().getDeveloperType() == DeveloperType.VALUE);
            BigDecimal popularMarketCap = calculateMarketCapSum(holdBunnies, 
                holdBunny -> holdBunny.getBunny().getDeveloperType() == DeveloperType.POPULAR);
            BigDecimal balanceMarketCap = calculateMarketCapSum(holdBunnies, 
                holdBunny -> holdBunny.getBunny().getDeveloperType() == DeveloperType.BALANCE);
            
            return DeveloperTypeStats.builder()
                .basic(calculateRatio(basicMarketCap, totalMarketCap))
                .growth(calculateRatio(growthMarketCap, totalMarketCap))
                .stable(calculateRatio(stableMarketCap, totalMarketCap))
                .value(calculateRatio(valueMarketCap, totalMarketCap))
                .popular(calculateRatio(popularMarketCap, totalMarketCap))
                .balance(calculateRatio(balanceMarketCap, totalMarketCap))
                .top(findTopField(holdBunnies, DeveloperType.class, holdBunny -> holdBunny.getBunny().getDeveloperType()))
                .build();
        }
    }

    @Builder
    public static record CoinTypeStats(
        double a,
        double b,
        double c,
        TopStats top
    ) {
        public static CoinTypeStats from(List<HoldBunny> holdBunnies, BigDecimal totalMarketCap) {
            
            
            BigDecimal aMarketCap = calculateMarketCapSum(holdBunnies, 
                holdBunny -> holdBunny.getBunny().getBunnyType() == BunnyType.A);
            BigDecimal bMarketCap = calculateMarketCapSum(holdBunnies, 
                holdBunny -> holdBunny.getBunny().getBunnyType() == BunnyType.B);
            BigDecimal cMarketCap = calculateMarketCapSum(holdBunnies, 
                holdBunny -> holdBunny.getBunny().getBunnyType() == BunnyType.C);

            return CoinTypeStats.builder()
                .a(calculateRatio(aMarketCap, totalMarketCap))
                .b(calculateRatio(bMarketCap, totalMarketCap))
                .c(calculateRatio(cMarketCap, totalMarketCap))
                .top(findTopField(holdBunnies, BunnyType.class, holdBunny -> holdBunny.getBunny().getBunnyType()))
                .build();
        }
    }
    
    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public static record TopStats(
        String type,
        BigDecimal totalMarketCap
    ) {}

    public static HoldBunniesStatsResponse from(List<HoldBunny> holdBunnies) {
        BigDecimal totalMarketCap = calculateTotalMarketCap(holdBunnies);
        return HoldBunniesStatsResponse.builder()
                .timestamp(LocalDateTime.now())
                .totalMarketCap(totalMarketCap)
                .position(PositionStats.from(holdBunnies, totalMarketCap))
                .developerType(DeveloperTypeStats.from(holdBunnies, totalMarketCap))
                .coinType(CoinTypeStats.from(holdBunnies, totalMarketCap))
                .build();
    }
}