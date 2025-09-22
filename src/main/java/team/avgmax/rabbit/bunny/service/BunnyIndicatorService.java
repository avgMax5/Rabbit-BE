package team.avgmax.rabbit.bunny.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import team.avgmax.rabbit.bunny.entity.Bunny;
import team.avgmax.rabbit.bunny.entity.BunnyHistory;
import team.avgmax.rabbit.bunny.repository.BunnyHistoryRepository;
import team.avgmax.rabbit.user.entity.Career;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class BunnyIndicatorService {
    
    private final BunnyHistoryRepository bunnyHistoryRepository;

    // ==================== Public Update Methods ====================
    
    public void updateBunnyReliability(Bunny bunny) {
        double reliability = calculateReliability(bunny);
        bunny.updateReliability(reliability);
    }
    
    public void updateBunnyGrowth(Bunny bunny) {
        double growth = calculateAnnualizedGrowthRate(bunny.getId());
        bunny.updateGrowth(growth);
    }
    
    public void updateBunnyStability(Bunny bunny) {
        double stability = calculateVolatility30dPct(bunny.getId());
        bunny.updateStability(stability);
    }
    
    public void updateBunnyValue(Bunny bunny) {
        double value = calculateValueScore(bunny);
        bunny.updateValue(value);
    }
    
    public void updateBunnyPopularity(Bunny bunny) {
        double popularity = calculatePopularityScore(bunny);
        bunny.updatePopularity(popularity);
    }
    
    public void updateBunnyBalance(Bunny bunny) {
        double balance = calculateBalanceScore(bunny);
        bunny.updateBalance(balance);
    }

    // ==================== Reliability Calculation ====================
    
    private double calculateReliability(Bunny bunny) {
        double skillScore = calculateSkillScore(bunny);
        double marketScore = calculateMarketScore(bunny);
        double reputationScore = calculateReputationScore(bunny);
        
        double total = skillScore * 0.4 + marketScore * 0.3 + reputationScore * 0.3;
        return Math.max(0, Math.min(100, Math.round(total)));
    }
    
    private double calculateSkillScore(Bunny bunny) {
        double score = 0;
        
        // 경력 점수: 0~10년을 0~40점으로 선형 변환
        double years = calculateCareerYears(bunny);
        double careerScore = Math.min(40, years / 10.0 * 40);
        score += careerScore;

        // 기술 스택 점수: 0~5개를 0~5점으로 선형 변환
        int techCount = bunny.getUser().getSkill().size();
        double techScore = Math.min(5, techCount / 5.0 * 5);
        score += techScore;
        
        // 자격증 점수: 0~3개를 0~5점으로 선형 변환
        int certCount = bunny.getUser().getCertification().size();
        double certScore = Math.min(5, certCount / 3.0 * 5);
        score += certScore;

        return score;
    }
    
    private double calculateMarketScore(Bunny bunny) {
        double score = 0;
        double growth = bunny.getGrowth();
        double volatility = bunny.getStability();
        double liquidity = bunny.getBalance();

        // 성장률 점수: 0~100%를 0~30점으로 선형 변환
        double growthScore = Math.min(30, Math.max(0, growth) / 100.0 * 30);
        score += growthScore;

        // 변동성 점수: 0~50%를 30~0점으로 선형 변환 (낮을수록 좋음)
        double volatilityScore = Math.max(0, 30 - (Math.max(0, volatility) / 50.0 * 30));
        score += volatilityScore;

        // 유동성 점수: 0~100%를 0~10점으로 선형 변환
        double liquidityScore = Math.min(10, Math.max(0, liquidity) / 100.0 * 10);
        score += liquidityScore;

        return score;
    }
    
    private double calculateReputationScore(Bunny bunny) {
        double score = 0;

        // 인증 점수: 증명서 비율을 0~10점으로 선형 변환
        long totalItems = bunny.getUser().getCareer().size() + bunny.getUser().getCertification().size();
        if (totalItems > 0) {
            long verifiedCount = bunny.getUser().getCareer().stream()
                    .mapToLong(c -> c.getCertificateUrl() != null && !c.getCertificateUrl().isBlank() ? 1 : 0)
                    .sum() +
                    bunny.getUser().getCertification().stream()
                    .mapToLong(c -> c.getCertificateUrl() != null && !c.getCertificateUrl().isBlank() ? 1 : 0)
                    .sum();
            
            double verificationScore = (double) verifiedCount / totalItems * 10;
            score += verificationScore;
        }

        // 좋아요 점수: 0~30개를 0~10점으로 선형 변환
        long likes = bunny.getLikeCount();
        double likeScore = Math.min(10, likes / 30.0 * 10);
        score += likeScore;

        // 업데이트 점수: 0~365일을 10~0점으로 선형 변환 (최신일수록 높음)
        long daysSinceUpdate = ChronoUnit.DAYS.between(bunny.getUser().getSpecUpdatedAt().toLocalDate(), LocalDate.now());
        double updateScore = Math.max(0, 10 - (daysSinceUpdate / 365.0 * 10));
        score += updateScore;

        return score;
    }
    
    private double calculateCareerYears(Bunny bunny) {
        List<Career> careers = bunny.getUser().getCareer();
        if (careers.isEmpty()) return 0.0;
        
        long totalDays = calculateMergedCareerDays(careers);
        return totalDays / 365.0;
    }
    
    private long calculateMergedCareerDays(List<Career> careers) {
        List<LocalDate[]> periods = careers.stream()
                .map(c -> new LocalDate[]{c.getStartDate(), c.getEndDate() != null ? c.getEndDate() : LocalDate.now()})
                .sorted(Comparator.comparing(arr -> arr[0]))
                .toList();

        long totalDays = 0;
        LocalDate currentStart = null, currentEnd = null;

        for (LocalDate[] period : periods) {
            if (currentStart == null) {
                currentStart = period[0];
                currentEnd = period[1];
            } else if (!period[0].isAfter(currentEnd)) {
                if (period[1].isAfter(currentEnd)) currentEnd = period[1];
            } else {
                totalDays += ChronoUnit.DAYS.between(currentStart, currentEnd);
                currentStart = period[0];
                currentEnd = period[1];
            }
        }

        if (currentStart != null) totalDays += ChronoUnit.DAYS.between(currentStart, currentEnd);
        return totalDays;
    }

    // ==================== Growth Calculation ====================
    
    private double calculateAnnualizedGrowthRate(String bunnyId) {
        List<BunnyHistory> histories = bunnyHistoryRepository.findAllByBunnyIdOrderByDateAsc(bunnyId);
        
        if (histories.size() < 2) return 0.0;
        
        BunnyHistory first = histories.get(0);
        BunnyHistory last = histories.get(histories.size() - 1);
        
        BigDecimal startValue = first.getClosingPrice();
        BigDecimal endValue = last.getClosingPrice();
        
        if (startValue == null || endValue == null || startValue.compareTo(BigDecimal.ZERO) == 0) {
            return 0.0;
        }
        
        long days = ChronoUnit.DAYS.between(first.getDate(), last.getDate());
        if (days <= 0) return 0.0;
        
        double ratio = endValue.doubleValue() / startValue.doubleValue();
        double annualized = Math.pow(ratio, 365.0 / days) - 1.0;
        
        return annualized * 100;
    }

    // ==================== Stability Calculation ====================
    
    private double calculateVolatility30dPct(String bunnyId) {
        List<BunnyHistory> histories = bunnyHistoryRepository.findRecentByBunnyIdOrderByDateAsc(bunnyId, 31);
        
        if (histories.size() < 2) return 0.0;
        
        List<Double> returns = new ArrayList<>();
        for (int i = 1; i < histories.size(); i++) {
            BigDecimal prev = histories.get(i - 1).getClosingPrice();
            BigDecimal curr = histories.get(i).getClosingPrice();
            
            if (prev == null || curr == null || prev.compareTo(BigDecimal.ZERO) == 0) {
                continue;
            }
            
            double r = Math.log(curr.doubleValue() / prev.doubleValue());
            returns.add(r);
        }
        
        if (returns.isEmpty()) return 0.0;
        
        double mean = returns.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
        double variance = returns.stream()
                .mapToDouble(r -> Math.pow(r - mean, 2))
                .sum() / returns.size();
        double stdDev = Math.sqrt(variance);
        
        return stdDev * 100;
    }

    // ==================== Value Calculation ====================
    
    private double calculateValueScore(Bunny bunny) {
        double score = 0;
        
        // 시가총액 점수: 0~200억을 0~50점으로 선형 변환
        BigDecimal marketCap = bunny.getMarketCap();
        if (marketCap != null) {
            double marketCapScore = Math.min(50, marketCap.doubleValue() / 20_000_000_000.0 * 50);
            score += marketCapScore;
        }
        
        // 신뢰도 점수: 0~100점을 0~50점으로 선형 변환
        double reliability = bunny.getReliability();
        double reliabilityScore = reliability / 100.0 * 50;
        score += reliabilityScore;
        
        return Math.min(100, score);
    }

    // ==================== Popularity Calculation ====================
    
    private double calculatePopularityScore(Bunny bunny) {
        double score = 0;
        
        double tradingScore = calculateTradingVolumeScore(bunny);
        score += tradingScore;
        
        double likeScore = calculateLikeCountScore(bunny.getLikeCount());
        score += likeScore;
        
        return Math.min(100, score);
    }
    
    private double calculateTradingVolumeScore(Bunny bunny) {
        List<BunnyHistory> histories = bunnyHistoryRepository.findRecentByBunnyIdOrderByDateAsc(bunny.getId(), 30);
        
        if (histories.isEmpty()) return 0.0;
        
        double totalVolume = histories.stream()
                .mapToDouble(h -> {
                    BigDecimal tradeQty = h.getTradeQuantity();
                    return (tradeQty != null && tradeQty.compareTo(BigDecimal.ZERO) > 0) 
                            ? tradeQty.doubleValue() 
                            : 0.0;
                })
                .sum();
        
        double avgDailyVolume = totalVolume / histories.size();
        
        // BunnyType별 총 발행량에 따른 거래량 비율 계산
        BigDecimal totalSupply = bunny.getBunnyType().getTotalSupply();
        double tradingRatio = avgDailyVolume / totalSupply.doubleValue();
        
        // 거래량 비율을 0~60점으로 선형 변환 (최대 2% 기준)
        return Math.min(60, tradingRatio / 0.02 * 60);
    }
    
    private double calculateLikeCountScore(long likeCount) {
        return Math.min(40, likeCount / 100.0 * 40);
    }

    // ==================== Balance Calculation ====================
    
    private double calculateBalanceScore(Bunny bunny) {
        double score = 0;
        
        double marketCapScore = calculateMarketCapBalanceScore(bunny.getMarketCap());
        score += marketCapScore;
        
        double growthScore = calculateGrowthBalanceScore(bunny.getGrowth());
        score += growthScore;
        
        double stabilityScore = calculateStabilityBalanceScore(bunny.getStability());
        score += stabilityScore;
        
        return Math.min(100, score);
    }
    
    private double calculateMarketCapBalanceScore(BigDecimal marketCap) {
        if (marketCap == null) return 0.0;
        
        double cap = marketCap.doubleValue();
        
        // 이상적인 가격대: 10억 ~ 50억에서 최대 점수
        if (cap >= 1_000_000_000 && cap <= 5_000_000_000.0) {
            return 40.0;
        } else if (cap < 1_000_000_000) {
            // 0~10억: 선형 증가
            return cap / 1_000_000_000 * 40;
        } else {
            // 50억 초과: 선형 감소 (최대 100억까지)
            return Math.max(0, 40 - (cap - 5_000_000_000.0) / 5_000_000_000.0 * 40);
        }
    }
    
    private double calculateGrowthBalanceScore(double growth) {
        // 이상적인 성장률: 10% ~ 30%에서 최대 점수
        if (growth >= 10 && growth <= 30) {
            return 30.0;
        } else if (growth < 10) {
            // 0~10%: 선형 증가
            return Math.max(0, growth / 10.0 * 30);
        } else {
            // 30% 초과: 선형 감소 (최대 60%까지)
            return Math.max(0, 30 - (growth - 30) / 30.0 * 30);
        }
    }
    
    private double calculateStabilityBalanceScore(double stability) {
        // 이상적인 변동성: 5% ~ 15%에서 최대 점수
        if (stability >= 5 && stability <= 15) {
            return 30.0;
        } else if (stability < 5) {
            // 0~5%: 선형 증가
            return Math.max(0, stability / 5.0 * 30);
        } else {
            // 15% 초과: 선형 감소 (최대 30%까지)
            return Math.max(0, 30 - (stability - 15) / 15.0 * 30);
        }
    }
}