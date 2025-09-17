package team.avgmax.rabbit.bunny.service;

import com.querydsl.core.Tuple;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;
import team.avgmax.rabbit.bunny.dto.data.ComparisonData;
import team.avgmax.rabbit.bunny.dto.data.DailyPriceData;
import team.avgmax.rabbit.bunny.dto.data.MyBunnyByDevTypeData;
import team.avgmax.rabbit.bunny.dto.data.MyBunnyByHolderData;
import team.avgmax.rabbit.bunny.dto.request.OrderRequest;
import team.avgmax.rabbit.bunny.dto.response.ChartDataPoint;
import team.avgmax.rabbit.bunny.dto.response.ChartResponse;
import team.avgmax.rabbit.bunny.dto.response.FetchBunnyResponse;
import team.avgmax.rabbit.bunny.dto.response.MyBunnyResponse;
import team.avgmax.rabbit.bunny.dto.response.OrderListResponse;
import team.avgmax.rabbit.bunny.dto.response.OrderResponse;
import team.avgmax.rabbit.bunny.entity.*;
import team.avgmax.rabbit.bunny.entity.enums.BunnyFilter;
import team.avgmax.rabbit.bunny.entity.enums.BunnyType;
import team.avgmax.rabbit.bunny.entity.enums.OrderType;
import team.avgmax.rabbit.bunny.entity.enums.ChartInterval;
import team.avgmax.rabbit.bunny.entity.enums.DeveloperType;
import team.avgmax.rabbit.bunny.exception.BunnyError;
import team.avgmax.rabbit.bunny.exception.BunnyException;
import team.avgmax.rabbit.bunny.repository.*;
import team.avgmax.rabbit.global.policy.FeePolicy;
import team.avgmax.rabbit.user.dto.response.SpecResponse;
import team.avgmax.rabbit.user.entity.HoldBunny;
import team.avgmax.rabbit.user.repository.PersonalUserRepository;
import team.avgmax.rabbit.user.entity.PersonalUser;
import team.avgmax.rabbit.user.exception.UserError;
import team.avgmax.rabbit.user.exception.UserException;
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
    private final BunnyLikeRepository bunnyLikeRepository;
    private final PersonalUserRepository personalUserRepository;
    private final OrderRepository orderRepository;
    private final MatchRepository matchRepository;


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
    public MyBunnyResponse getMyBunny(String userId) {
        PersonalUser personalUser = personalUserRepository.findById(userId)
                .orElseThrow(() -> new UserException(UserError.USER_NOT_FOUND));
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
                .likeCount(myBunny.getLikeCount())
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

    // 특정 버니 마이 리스트 조회
    @Transactional(readOnly = true)
    public OrderListResponse getMyBunnyList(String bunnyName, String userId) {
        Bunny bunny = findBunnyByName(bunnyName);

        List<Order> orders = orderRepository.findAllByBunnyIdAndUserId(bunny.getId(), userId);

        List<OrderResponse> ordersResponse = orders.stream()
                .map(OrderResponse::from)
                .toList();

        OrderListResponse myBunnyList = OrderListResponse.from(ordersResponse);
        return myBunnyList;
    }

    // 좋아요 추가
    @Transactional
    public void addBunnyLike(String bunnyName, String userId) {
        Bunny bunny = findBunnyByName(bunnyName);
        bunnyLikeRepository.save(BunnyLike.create(bunny.getId(), userId));
        bunny.addLikeCount();
    }

    // 좋아요 취소
    @Transactional
    public void cancelBunnyLike(String bunnyName, String userId) {
        Bunny bunny = findBunnyByName(bunnyName);
        bunnyLikeRepository.deleteByBunnyIdAndUserId(bunny.getId(), userId);
        bunny.subtractLikeCount();
    }

    private Bunny findBunnyByName(String bunnyName) {
        return bunnyRepository.findByBunnyName(bunnyName)
                .orElseThrow(() -> new BunnyException(BunnyError.BUNNY_NOT_FOUND));
    }

    // 거래 주문 요청
    @Transactional
    public OrderResponse createOrder(String bunnyName, OrderRequest request, PersonalUser principal) {
        Bunny bunny = bunnyRepository.findByBunnyName(bunnyName)
                .orElseThrow(() -> new BunnyException(BunnyError.BUNNY_NOT_FOUND));

        PersonalUser user = personalUserRepository.findByIdForUpdate(principal.getId());

        if (request.orderType() == OrderType.SELL) {
            holdBunnyRepository.findByHolderAndBunnyForUpdate(user, bunny);
        }

        switch (request.orderType()) {
            case BUY -> validateBuy(request, user);
            case SELL -> validateSell(bunny, request, user);
            default -> throw new IllegalArgumentException("지원하지 않는 타입");
        }

        Order order = request.toEntity(user, bunny);
        orderRepository.save(order);

        List<Order> candidates = (order.getOrderType() == OrderType.BUY)
                ? orderRepository.findSellCandidatesByPriceAsc(bunny.getId(), order.getUnitPrice(), user.getId())
                : orderRepository.findBuyCandidatesByPriceDesc(bunny.getId(), order.getUnitPrice(), user.getId());

        BigDecimal remainingQty = order.getQuantity();

        for (Order counter : candidates) {
            if (isZero(remainingQty)) break;

            BigDecimal counterOwnFilled = ownFilledFor(counter, bunny.getId());
            BigDecimal counterRemaining = counter.getQuantity().subtract(counterOwnFilled);
            if (counterRemaining.signum() <= 0) continue;

            BigDecimal tradable = min(remainingQty, counterRemaining);
            if (tradable.signum() <= 0) continue;

            BigDecimal tradePrice = counter.getUnitPrice();
            BigDecimal tradeAmount = tradable.multiply(tradePrice);

            Match match = Match.builder()
                    .bunny(bunny)
                    .buyUser(order.getOrderType() == OrderType.BUY ? order.getUser() : counter.getUser())
                    .sellUser(order.getOrderType() == OrderType.SELL ? order.getUser() : counter.getUser())
                    .quantity(tradable)
                    .unitPrice(tradePrice)
                    .build();
            matchRepository.save(match);

            PersonalUser buyer  = match.getBuyUser();
            PersonalUser seller = match.getSellUser();

            // 매수자: 보유 수량 증가 (BUY는 캐럿/수수료는 동결로 처리했으므로 여기서는 수량만)
            holdBunnyRepository.addHoldForUpdate(buyer.getId(), bunny.getId(), tradable);

            // 매도자: 보유 수량 감소 + 캐럿 입금(수수료 차감)
            BigDecimal sellerFee = FeePolicy.calcFee(tradeAmount);
            holdBunnyRepository.addHoldForUpdate(seller.getId(), bunny.getId(), tradable.negate());
            personalUserRepository.addCarrotForUpdate(seller.getId(), tradeAmount.subtract(sellerFee));

            remainingQty = remainingQty.subtract(tradable);
        }

        return OrderResponse.from(order);
    }

    @Transactional
    public void cancelOrder(String bunnyName, String orderId, PersonalUser principal) {
        Bunny bunny = bunnyRepository.findByBunnyName(bunnyName)
                .orElseThrow(() -> new BunnyException(BunnyError.BUNNY_NOT_FOUND));

        Order order = orderRepository.findByIdAndBunnyIdForUpdate(orderId, bunny.getId());
        if (order == null) throw new BunnyException(BunnyError.ORDER_NOT_FOUND);

        if (!order.getUser().getId().equals(principal.getId())) {
            throw new BunnyException(BunnyError.FORBIDDEN);
        }

        BigDecimal remaining = remainingOf(order, bunny.getId());
        if (isZero(remaining)) {
            throw new BunnyException(BunnyError.ORDER_ALREADY_FILLED);
        }

        orderRepository.delete(order);
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

    private void validateBuy(OrderRequest request, PersonalUser user) {
        // 주문 총액(수수료 포함) = 수량 × 단가 × (1 + feeRate)
        BigDecimal orderCost = request.quantity().multiply(request.unitPrice()).multiply(BigDecimal.ONE.add(FeePolicy.FEE_RATE));

        // 미체결 매수 주문들의 동결 금액(수수료 포함)
        BigDecimal reserved = sumOpenBuyReservedAmount(user.getId());

        BigDecimal available = user.getCarrot().subtract(reserved);

        if (available.compareTo(orderCost) < 0) throw new BunnyException(BunnyError.INSUFFICIENT_BALANCE);
    }

    private void validateSell(Bunny bunny, OrderRequest request, PersonalUser user) {
        // 현재 보유 수량
        BigDecimal holding = holdBunnyRepository.findByHolderAndBunny(user, bunny)
                .map(HoldBunny::getHoldQuantity)
                .orElse(BigDecimal.ZERO);

        // 이미 걸려 있는 매도 주문들의 잔여 수량 합 (예약된 수량)
        BigDecimal reservedQty = sumOpenSellReservedQty(user.getId(), bunny.getId());

        BigDecimal availableQty = holding.subtract(reservedQty);

        if (availableQty.compareTo(request.quantity()) < 0) throw new BunnyException(BunnyError.INSUFFICIENT_HOLDING);
    }

    private BigDecimal remainingOf(Order order, String bunnyId) {
        return order.getQuantity().subtract(ownFilledFor(order, bunnyId));
    }

    private BigDecimal ownFilledFor(Order order, String bunnyId) {
        BigDecimal cum = matchRepository.sumFilledByUserSideAndPrice(
                bunnyId,
                order.getUser().getId(),
                order.getOrderType(),
                order.getUnitPrice()
        );
        BigDecimal prev = orderRepository.sumPrevOrdersQuantity(
                bunnyId,
                order.getUser().getId(),
                order.getOrderType(),
                order.getUnitPrice(),
                order.getCreatedAt()
        );

        BigDecimal own = cum.subtract(prev);
        if (own.signum() < 0) own = BigDecimal.ZERO;
        if (own.compareTo(order.getQuantity()) > 0) own = order.getQuantity();
        return own;
    }

    private BigDecimal sumOpenBuyReservedAmount(String userId) {
        List<Order> orders = orderRepository
                .findAllByUserAndSideOrderByCreatedAtAsc(userId, OrderType.BUY);

        BigDecimal total = BigDecimal.ZERO;
        for (Order order : orders) {
            BigDecimal rem = remainingOf(order, order.getBunny().getId());
            if (rem.signum() > 0) {
                BigDecimal costWithFee = rem
                        .multiply(order.getUnitPrice())
                        .multiply(BigDecimal.ONE.add(FeePolicy.FEE_RATE));
                total = total.add(costWithFee);
            }
        }
        return total;
    }

    private BigDecimal sumOpenSellReservedQty(String userId, String bunnyId) {
        List<Order> orders = orderRepository
                .findAllByUserAndBunnyAndSideOrderByCreatedAtAsc(userId, bunnyId, OrderType.SELL);

        BigDecimal total = BigDecimal.ZERO;
        for (Order o : orders) {
            BigDecimal rem = remainingOf(o, bunnyId);
            if (rem.signum() > 0) {
                total = total.add(rem); // 잔여 수량 누적
            }
        }
        return total;
    }

    private static boolean isZero(BigDecimal v) {
        return v == null || v.compareTo(BigDecimal.ZERO) == 0;
    }
    private static BigDecimal min(BigDecimal a, BigDecimal b) {
        return a.compareTo(b) <= 0 ? a : b;
    }
}
