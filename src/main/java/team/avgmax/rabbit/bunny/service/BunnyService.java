package team.avgmax.rabbit.bunny.service;

import com.querydsl.core.Tuple;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;
import team.avgmax.rabbit.auth.oauth2.CustomOAuth2User;
import team.avgmax.rabbit.bunny.dto.data.ComparisonData;
import team.avgmax.rabbit.bunny.dto.data.DailyPriceData;
import team.avgmax.rabbit.bunny.dto.data.MyBunnyByDevTypeData;
import team.avgmax.rabbit.bunny.dto.data.MyBunnyByHolderData;
import team.avgmax.rabbit.bunny.dto.request.OrderRequest;
import team.avgmax.rabbit.bunny.dto.response.ChartDataPoint;
import team.avgmax.rabbit.bunny.dto.response.ChartResponse;
import team.avgmax.rabbit.bunny.dto.response.FetchBunnyResponse;
import team.avgmax.rabbit.bunny.dto.response.MyBunnyResponse;
import team.avgmax.rabbit.bunny.entity.Badge;
import team.avgmax.rabbit.bunny.entity.Bunny;
import team.avgmax.rabbit.bunny.entity.BunnyHistory;
import team.avgmax.rabbit.bunny.entity.Order;
import team.avgmax.rabbit.bunny.entity.enums.BunnyFilter;
import team.avgmax.rabbit.bunny.entity.enums.BunnyType;
import team.avgmax.rabbit.bunny.entity.enums.ChartInterval;
import team.avgmax.rabbit.bunny.entity.enums.DeveloperType;
import team.avgmax.rabbit.bunny.exception.BunnyError;
import team.avgmax.rabbit.bunny.exception.BunnyException;
import team.avgmax.rabbit.bunny.repository.BadgeRepository;
import team.avgmax.rabbit.bunny.repository.BunnyHistoryRepository;
import team.avgmax.rabbit.bunny.repository.BunnyRepository;
import team.avgmax.rabbit.bunny.repository.OrderRepository;
import team.avgmax.rabbit.user.dto.response.OrderResponse;
import team.avgmax.rabbit.user.dto.response.SpecResponse;
import team.avgmax.rabbit.user.entity.PersonalUser;
import team.avgmax.rabbit.user.repository.HoldBunnyRepository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class BunnyService {

    private final BunnyRepository bunnyRepository;
    private final BadgeRepository badgeRepository;
    private final BunnyHistoryRepository bunnyHistoryRepository;
    private final HoldBunnyRepository holdBunnyRepository;
    private final OrderRepository orderRepository;


    // 버니 목록 조회
    @Transactional(readOnly = true) // 읽기 전용
    public List<FetchBunnyResponse> getBunniesByFilter(BunnyFilter filter) {
        List<Bunny> bunnies = switch (filter) {
            case ALL -> bunnyRepository.findAllByPriorityGroupAndCreatedAt(); // 로켓 탑승한 버니들
            case LATEST -> bunnyRepository.findAllByOrderByCreatedAtDesc(); // GOT 탑승한 버니들
            case CAPITALIZATION -> bunnyRepository.findTop5ByOrderByMarketCapDesc(); // Top 5 버니들
        };

        if (bunnies.isEmpty()) {
            log.debug("No bunnies found for filter={}", filter);
        }

        List<String> bunnyIds = bunnies.stream().map(Bunny::getId).toList();

        Map<String, List<Badge>> badgesByBunnyId = badgeRepository.findAllByBunnyIdIn(bunnyIds).stream()
                .collect(Collectors.groupingBy(Badge::getBunnyId));

        return bunnies.stream()
                .map(bunny -> {
                    List<Badge> badges = badgesByBunnyId.getOrDefault(bunny.getId(), Collections.emptyList());
                    return FetchBunnyResponse.from(bunny,badges);
                })
                .toList();
    }

    // 버니 상세 조회
    @Transactional(readOnly = true)
    public FetchBunnyResponse getBunnyByName(String bunnyName) {
        Bunny bunny = bunnyRepository.findByBunnyName(bunnyName)
                .orElseThrow(() -> new BunnyException(BunnyError.BUNNY_NOT_FOUND));

        List<Badge> badges = badgeRepository.findAllByBunnyId(bunny.getId());

        log.debug("Found bunny id={} name={}", bunny.getId(), bunny.getBunnyName());

        return FetchBunnyResponse.from(bunny, badges);
    }

    // 마이 버니 조회
    @Transactional(readOnly = true)
    public MyBunnyResponse getMyBunny(CustomOAuth2User customOAuth2User) {
        PersonalUser personalUser = customOAuth2User.getPersonalUser();
        Bunny myBunny = bunnyRepository.findByUserId(personalUser.getId())
                .orElseThrow(() -> new BunnyException(BunnyError.BUNNY_NOT_FOUND));

        List<DailyPriceData> priceHistory = getPriceHistory(myBunny.getId());
        List<ComparisonData> competitors = getCompetitors(myBunny);
        List<MyBunnyByDevTypeData> holderTypes = getHolderTypes(myBunny);
        List<MyBunnyByHolderData> holders = getHolders(myBunny.getId());
        List<Badge> badges = badgeRepository.findAllByBunnyId(myBunny.getId());
        SpecResponse spec = SpecResponse.from(personalUser);
        BigDecimal myGrowthRate = calculateGrowthRate(myBunny);
        BigDecimal avgBunnyTypeGrowthRate = bunnyRepository.findAverageGrowthRateByBunnyType(myBunny.getBunnyType());
        BigDecimal avgPositionGrowthRate = bunnyRepository.findAverageGrowthRateByPosition(myBunny.getPosition());
        BigDecimal avgDevTypeGrowthRate = bunnyRepository.findAverageGrowthRateByDeveloperType(myBunny.getDeveloperType());
        List<DailyPriceData> monthlyGrowRate = getMonthlyGrowthRate(myBunny.getId());

        return MyBunnyResponse.builder()
                .bunnyId(myBunny.getId())
                .userName(personalUser.getName())
                .userImage(personalUser.getImage())
                .userCarrot(personalUser.getCarrot())
                .bunnyName(myBunny.getBunnyName())
                .bunnyType(myBunny.getBunnyType())
                .developerType(myBunny.getDeveloperType())
                .position(myBunny.getPosition())
                .badges(badges)
                .todayTime(LocalDate.now())
                .monthlyGrowthRates(monthlyGrowRate)
                .priceHistory(priceHistory)
                .reliability(myBunny.getReliability()) // 추후 계산 로직 구상 시 구현
                .currentPrice(myBunny.getCurrentPrice())
                .closingPrice(myBunny.getClosingPrice())
                .marketCap(myBunny.getMarketCap())
                .myGrowthRate(myGrowthRate)
                .avgBunnyTypeVsMe(myGrowthRate.subtract(avgBunnyTypeGrowthRate))
                .avgPositionVsMe(myGrowthRate.subtract(avgPositionGrowthRate))
                .avgDevTypeVsMe(myGrowthRate.subtract(avgDevTypeGrowthRate))
                .competitors(competitors)
//                .indicator1()
//                .indicator2()
//                .indicator3()
//                .indicator4()
//                .indicator5()
                .holderTypes(holderTypes)
                .holders(holders)
//                .propensityMatchRate()
                .spec(spec)
                .aiReview(myBunny.getAiReview())
                .aiFeedback(myBunny.getAiFeedback())
                .build();
    }

    // 거래 차트 조회
    @Transactional(readOnly = true)
    public ChartResponse getChart(String bunnyName, ChartInterval interval) {
        Bunny bunny = bunnyRepository.findByBunnyName(bunnyName)
                .orElseThrow(() -> new BunnyException(BunnyError.BUNNY_NOT_FOUND));

        List<ChartDataPoint> chartData =
                Optional.ofNullable(bunnyHistoryRepository.findChartData(bunny.getId(), interval))
                        .orElseGet(Collections::emptyList);

        return ChartResponse.from(chartData, bunny.getBunnyName(), interval);
    }

    // 거래 주문 요청
    @Transactional(readOnly = true)
    public OrderResponse createOrder(String bunnyName, OrderRequest request, PersonalUser personalUser) {
        Bunny bunny = bunnyRepository.findByBunnyName(bunnyName)
                .orElseThrow(() -> new BunnyException(BunnyError.BUNNY_NOT_FOUND));

        Order order = request.toEntity(personalUser, bunny);

        orderRepository.save(order);

        return OrderResponse.from(order);
    }

    private List<DailyPriceData> getPriceHistory(String bunnyId) {
        List<BunnyHistory> bunnyHistories = bunnyHistoryRepository.findAllByBunnyIdOrderByDateAsc(bunnyId);

        return bunnyHistories.stream()
                .map(bunnyHistory -> DailyPriceData.builder()
                        .date(bunnyHistory.getDate())
                        .closingPrice(bunnyHistory.getClosingPrice())
                        .build())
                .toList();
    }

    private List<ComparisonData> getCompetitors(Bunny myBunny) {
        List<Bunny> rankedBunnies = bunnyRepository.findAllWithUserOrderByMarketCapDesc();

        int myRankIndex = findRankIndex(rankedBunnies, myBunny.getId());

        // 내 버니를 찾지 못했거나, 경쟁할 대상이 없으면 빈 리스트 반환
        if (myRankIndex == -1 || rankedBunnies.size() <= 1) {
            return Collections.emptyList();
        }

        List<ComparisonData> competitors = new ArrayList<>();

        // 바로 위 경쟁자
        if (myRankIndex > 0) {
            competitors.add(toComparisonData(rankedBunnies, myRankIndex - 1));
        }

        // 바로 아래 경쟁자
        if (myRankIndex < rankedBunnies.size() - 1) {
            competitors.add(toComparisonData(rankedBunnies, myRankIndex + 1));
        }

        return competitors;
    }

    private int findRankIndex(List<Bunny> rankedBunnies, String myBunnyId) {
        for (int i = 0; i < rankedBunnies.size(); i++) {
            if (Objects.equals(rankedBunnies.get(i).getId(), myBunnyId)) {
                return i;
            }
        }
        return -1;
    }

    private static ComparisonData toComparisonData(List<Bunny> rankedBunnies, int index) {
        Bunny competitor = rankedBunnies.get(index);

        // 성장률 = (현재가 - 종가) / 종가 * 100
        BigDecimal growthRate = calculateGrowthRate(competitor);

        String userImage = (competitor.getUser() != null) ? competitor.getUser().getImage() : null;

        return ComparisonData.builder()
                .bunnyId(competitor.getId())
                .bunnyName(competitor.getBunnyName())
                .userImage(userImage)
                .rank(index + 1) // 1-based rank
                .marketCap(competitor.getMarketCap())
                .growthRate(growthRate)
                .build();
    }

    private static BigDecimal calculateGrowthRate(Bunny bunny) {
        if (bunny == null
                || bunny.getClosingPrice() == null
                || bunny.getCurrentPrice() == null
                || bunny.getClosingPrice().compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }

        BigDecimal currentPrice = bunny.getCurrentPrice();
        BigDecimal closingPrice = bunny.getClosingPrice();

        return currentPrice.subtract(closingPrice)
                .divide(closingPrice, 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100));
    }

    private List<MyBunnyByDevTypeData> getHolderTypes(Bunny myBunny) {
        BunnyType bunnyType = myBunny.getBunnyType();
        if (bunnyType == null) {
            log.warn("Bunny ID `{}`의 BunnyType이 null 입니다.", myBunny.getId());
            return Collections.emptyList();
        }

        BigDecimal totalSupply = bunnyType.getTotalSupply();
        if (totalSupply == null || totalSupply.compareTo(BigDecimal.ZERO) == 0) {
            return Collections.emptyList();
        }

        List<Tuple> distribution =
                holdBunnyRepository.findHolderTypeDistributionByBunnyId(myBunny.getId());

        return distribution.stream()
                .filter(t -> t.get(1, BigDecimal.class) != null)
                .map(tuple -> {
                    DeveloperType devType = tuple.get(0, DeveloperType.class);
                    BigDecimal totalQuantity = tuple.get(1, BigDecimal.class);
                    Long holderCount = tuple.get(2, Long.class);

                    DeveloperType safeType =
                            (devType != null) ? devType : DeveloperType.UNDEFINED;

                    BigDecimal percentage = totalQuantity
                            .divide(totalSupply, 4, RoundingMode.HALF_UP)
                            .multiply(BigDecimal.valueOf(100));

                    return MyBunnyByDevTypeData.builder()
                            .developerType(safeType)
                            .percentage(percentage)
                            .count(holderCount != null ? holderCount : 0L)
                            .build();
                })
                .toList();
    }

    private List<MyBunnyByHolderData> getHolders(String bunnyId) {

        return holdBunnyRepository.findHoldersByBunnyId(bunnyId);
    }

    private List<DailyPriceData> getMonthlyGrowthRate(String bunnyId) {
        List<BunnyHistory> histories = bunnyHistoryRepository.findAllByBunnyIdOrderByDateAsc(bunnyId);
        if (histories.isEmpty()) {
            return Collections.emptyList();
        }

        // 월별 그룹핑
        Map<YearMonth, List<BunnyHistory>> historiesByMonth = histories.stream()
                .collect(Collectors.groupingBy(
                        h -> YearMonth.from(h.getDate()),
                        TreeMap::new,
                        Collectors.toList()
                ));

        // 월별 마지막 종가만 추출
        Map<YearMonth, BigDecimal> monthEndCloseByMonth = extractMonthEndClosingPrices(historiesByMonth);

        // 전월말 대비 성장률 계산
        List<DailyPriceData> result = new ArrayList<>();
        BigDecimal prevMonthEndClose = null;

        for (Map.Entry<YearMonth, BigDecimal> e : monthEndCloseByMonth.entrySet()) {
            YearMonth ym = e.getKey();
            BigDecimal thisMonthEndClose = e.getValue();

            BigDecimal growthRatePct = calculateGrowthRate(prevMonthEndClose, thisMonthEndClose);

            result.add(DailyPriceData.builder()
                    .date(ym.atEndOfMonth())
                    .closingPrice(growthRatePct)
                    .build());

            prevMonthEndClose = thisMonthEndClose;
        }

        return result;
    }

    private static Map<YearMonth, BigDecimal> extractMonthEndClosingPrices(Map<YearMonth, List<BunnyHistory>> historiesByMonth) {
        Map<YearMonth, BigDecimal> monthEndCloseByMonth = new TreeMap<>();
        for (Map.Entry<YearMonth, List<BunnyHistory>> e : historiesByMonth.entrySet()) {
            List<BunnyHistory> monthHistories = e.getValue();
            monthHistories.sort(Comparator.comparing(BunnyHistory::getDate));
            BigDecimal lastClose = monthHistories.get(monthHistories.size() - 1).getClosingPrice();
            monthEndCloseByMonth.put(e.getKey(), lastClose);
        }
        return monthEndCloseByMonth;
    }

    private static BigDecimal calculateGrowthRate(BigDecimal prev, BigDecimal current) {
        if (prev == null || prev.compareTo(BigDecimal.ZERO) == 0 || current == null) {
            return null;
        }
        return current.subtract(prev)
                .divide(prev, 6, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100))
                .setScale(2, RoundingMode.HALF_UP);
    }
}
