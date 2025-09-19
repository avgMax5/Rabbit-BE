package team.avgmax.rabbit.bunny.service;

import com.querydsl.core.Tuple;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import team.avgmax.rabbit.bunny.controller.webSocket.OrderBookPublisher;
import team.avgmax.rabbit.bunny.dto.data.ComparisonData;
import team.avgmax.rabbit.bunny.dto.data.DailyPriceData;
import team.avgmax.rabbit.bunny.dto.data.MyBunnyByDevTypeData;
import team.avgmax.rabbit.bunny.dto.data.MyBunnyByHolderData;
import team.avgmax.rabbit.bunny.dto.orderBook.OrderBookDiff;
import team.avgmax.rabbit.bunny.dto.orderBook.OrderBookLevel;
import team.avgmax.rabbit.bunny.dto.orderBook.OrderBookSnapshot;
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
import team.avgmax.rabbit.bunny.service.webSocket.OrderBookAssembler;
import team.avgmax.rabbit.global.policy.FeePolicy;
import team.avgmax.rabbit.user.dto.response.SpecResponse;
import team.avgmax.rabbit.user.entity.CorporationUser;
import team.avgmax.rabbit.user.entity.HoldBunny;
import team.avgmax.rabbit.user.repository.CorporationUserRepository;
import team.avgmax.rabbit.user.repository.PersonalUserRepository;
import team.avgmax.rabbit.user.entity.PersonalUser;
import team.avgmax.rabbit.user.entity.enums.Role;
import team.avgmax.rabbit.user.exception.UserError;
import team.avgmax.rabbit.user.exception.UserException;
import team.avgmax.rabbit.user.repository.HoldBunnyRepository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.*;
import java.util.stream.Collectors;

import static team.avgmax.rabbit.bunny.service.webSocket.OrderBookAssembler.normalizePrice;

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
    private final CorporationUserRepository corporationUserRepository;
    private final OrderBookAssembler orderBookAssembler;
    private final OrderBookPublisher orderBookPublisher;

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

        return bunnies.stream()
                .map(bunny -> FetchBunnyResponse.from(bunny))
                .toList();
    }

    // 버니 상세 조회
    @Transactional(readOnly = true)
    public FetchBunnyResponse getBunnyByName(String bunnyName) {
        Bunny bunny = bunnyRepository.findByBunnyName(bunnyName)
                .orElseThrow(() -> new BunnyException(BunnyError.BUNNY_NOT_FOUND));

        log.debug("Found bunny id={} name={}", bunny.getId(), bunny.getBunnyName());

        return FetchBunnyResponse.from(bunny);
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
        SpecResponse spec = SpecResponse.from(personalUser);
        BigDecimal myGrowthRate = calculateGrowthRate(myBunny);
        BigDecimal avgBunnyTypeGrowthRate = bunnyRepository.findAverageGrowthRateByBunnyType(myBunny.getBunnyType());
        BigDecimal avgPositionGrowthRate = bunnyRepository.findAverageGrowthRateByPosition(myBunny.getUser().getPosition());
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
                .position(myBunny.getUser().getPosition())
                .badges(myBunny.getBadges().stream().map(Badge::getBadgeImg).toList())
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
                .growth(myBunny.getGrowth())
                .stability(myBunny.getStability())
                .value(myBunny.getValue())
                .popularity(myBunny.getPopularity())
                .balance(myBunny.getBalance())
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
    public void addBunnyLike(String bunnyName, String userId, Role role) {
        Bunny bunny = findBunnyByName(bunnyName);
        bunnyLikeRepository.save(BunnyLike.create(bunny.getId(), userId));
        if (role == Role.ROLE_CORPORATION) {
            CorporationUser corporationUser = corporationUserRepository.findById(userId)
                .orElseThrow(() -> new UserException(UserError.USER_NOT_FOUND));
            badgeRepository.save(Badge.create(bunny.getId(), userId, corporationUser.getCorporationName()));
        }
        bunny.addLikeCount();
    }

    // 좋아요 취소
    @Transactional
    public void cancelBunnyLike(String bunnyName, String userId, Role role) {
        Bunny bunny = findBunnyByName(bunnyName);
        bunnyLikeRepository.deleteByBunnyIdAndUserId(bunny.getId(), userId);
        if (role == Role.ROLE_CORPORATION) {
            badgeRepository.deleteByBunnyIdAndUserId(bunny.getId(), userId);
            return;
        }
        bunny.subtractLikeCount();
    }

    private Bunny findBunnyByName(String bunnyName) {
        return bunnyRepository.findByBunnyName(bunnyName)
                .orElseThrow(() -> new BunnyException(BunnyError.BUNNY_NOT_FOUND));
    }

    // 거래 주문 요청
    @Transactional
    public OrderResponse createOrder(String bunnyName, OrderRequest request, String userId) {
        Bunny bunny = bunnyRepository.findByBunnyName(bunnyName)
                .orElseThrow(() -> new BunnyException(BunnyError.BUNNY_NOT_FOUND));

        PersonalUser user = personalUserRepository.findByIdForUpdate(userId);

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

        final Set<BigDecimal> touchedBidPrices = new HashSet<>();
        final Set<BigDecimal> touchedAskPrices = new HashSet<>();

        if (order.getOrderType() == OrderType.BUY) {
            touchedBidPrices.add(order.getUnitPrice());
        } else {
            touchedAskPrices.add(order.getUnitPrice());
        }

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

            // 상대 가격대도 터치셋에 추가
            if (counter.getOrderType() == OrderType.BUY) {
                touchedBidPrices.add(counter.getUnitPrice());
            } else {
                touchedAskPrices.add(counter.getUnitPrice());
            }

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

        emitOrderBookDiff(bunny, touchedBidPrices, touchedAskPrices);

        return OrderResponse.from(order);
    }

    // 거래 주문 취소
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

        // 취소 처리
        orderRepository.delete(order);

        // 영향 받은 가격 레벨대 수집
        Set<BigDecimal> bidTouched = new HashSet<>();
        Set<BigDecimal> askTouched = new HashSet<>();
        if (order.getOrderType() == OrderType.BUY) {
            bidTouched.add(order.getUnitPrice());
        } else {
            askTouched.add(order.getUnitPrice());
        }

        // Diff 발행 (커밋 후 전송)
        emitOrderBookDiff(bunny, bidTouched, askTouched);
    }

    // 특정 버니 호가창 스냅샷 조회
    @Transactional(readOnly = true)
    public OrderBookSnapshot getOrderBookSnapshot(String bunnyName) {
        Bunny bunny = bunnyRepository.findByBunnyName(bunnyName)
                .orElseThrow(() -> new BunnyException(BunnyError.BUNNY_NOT_FOUND));

        List<Order> buyOrders  = orderRepository.findAllByBunnyAndSideForOrderBook(bunny.getId(), OrderType.BUY);
        List<Order> sellOrders = orderRepository.findAllByBunnyAndSideForOrderBook(bunny.getId(), OrderType.SELL);

        // Order → OrderLeaf 변환 (잔여 수량 계산 포함)
        List<OrderBookAssembler.OrderLeaf> bidLeaves = toLeaves(buyOrders, bunny.getId());
        List<OrderBookAssembler.OrderLeaf> askLeaves = toLeaves(sellOrders, bunny.getId());

        List<OrderBookLevel> bids = orderBookAssembler.toLevel(bidLeaves);
        List<OrderBookLevel> asks = orderBookAssembler.toLevel(askLeaves);

        BigDecimal currentPrice = queryCurrentPrice(bunny);

        return OrderBookSnapshot.from(bunny, bids, asks, currentPrice);
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

        user.subtractCarrot(reserved);

//        BigDecimal available = user.getCarrot().subtract(reserved);
//
//        if (available.compareTo(orderCost) < 0) throw new BunnyException(BunnyError.INSUFFICIENT_BALANCE);
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

        BigDecimal own = (cum == null ? BigDecimal.ZERO : cum).subtract(prev == null ? BigDecimal.ZERO : prev);
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

    private List<OrderBookAssembler.OrderLeaf> toLeaves(List<Order> orders, String bunnyId) {
        List<OrderBookAssembler.OrderLeaf> out = new ArrayList<>(orders.size());
        for (Order o : orders) {
            BigDecimal rem = remainingOf(o, bunnyId);
            if (rem.signum() > 0) {
                out.add(new OrderBookAssembler.OrderLeaf(o.getUnitPrice(), rem));
            }
        }
        return out;
    }

    private BigDecimal queryCurrentPrice(Bunny bunny) {
        // 1) 최근 체결가 (동일 트랜잭션 내에서 방금 저장한 Match 도 조회)
        BigDecimal lastTrade = matchRepository.findLastTradePriceByBunnyId(bunny.getId());
        if (lastTrade != null) return lastTrade;

        // 2) 저장된 현재가(있다면)
        if (bunny.getCurrentPrice() != null) return bunny.getCurrentPrice();

        // 3) 직전 종가(있다면)
        if (bunny.getClosingPrice() != null) return bunny.getClosingPrice();

        // 4) 정말 없으면 0
        return BigDecimal.ZERO;
    }

    private void emitOrderBookDiff(Bunny bunny, Set<BigDecimal> bidPrices, Set<BigDecimal> askPrices) {
        if ((bidPrices == null || bidPrices.isEmpty()) && (askPrices == null || askPrices.isEmpty())) {
            return;
        }

        // 부분 집계들 (Upserts/Deletes)
        // 잔여가 있으면 → upsert
        List<OrderBookLevel> bidUpserts = aggregateLevelsForPrices(bunny.getId(), OrderType.BUY, bidPrices);
        List<OrderBookLevel> askUpserts = aggregateLevelsForPrices(bunny.getId(), OrderType.SELL, askPrices);

        // 잔여가 0이면 → delete
        List<BigDecimal> bidDeletes = findDeletes(bidPrices, bidUpserts);
        List<BigDecimal> askDeletes = findDeletes(askPrices, askUpserts);

        BigDecimal currentPrice = queryCurrentPrice(bunny);

        OrderBookDiff diff = new OrderBookDiff(
                bunny.getBunnyName(),
                bidUpserts,
                bidDeletes,
                askUpserts,
                askDeletes,
                currentPrice,
                System.currentTimeMillis()
        );

        // 트랜잭션 커밋 이후에만 브로드캐스트
        publishAfterCommit(bunny.getBunnyName(), diff);
    }

    private List<OrderBookLevel> aggregateLevelsForPrices(String bunnyId, OrderType side, Set<BigDecimal> prices) {
        if (prices == null || prices.isEmpty()) return List.of();

        Set<BigDecimal> normPrices = prices.stream()
                .filter(Objects::nonNull)
                .map(OrderBookAssembler::normalizePrice)
                .collect(Collectors.toSet());

        // 가격별 잔여량 합산
        Map<BigDecimal, BigDecimal> sumByPrice = orderRepository
                .findAllByBunnySideAndPriceIn(bunnyId, side, prices).stream()
                .map(o -> new AbstractMap.SimpleEntry<>(normalizePrice(o.getUnitPrice()), remainingOf(o, bunnyId)))
                .filter(e -> e.getValue() != null && e.getValue().signum() > 0)
                .collect(Collectors.groupingBy(
                        Map.Entry::getKey,
                        Collectors.mapping(Map.Entry::getValue,
                                Collectors.reducing(BigDecimal.ZERO, BigDecimal::add))
                ));

        // Map → OrderBookLevel 변환
        return sumByPrice.entrySet().stream()
                .map(e -> new OrderBookLevel(e.getKey(), e.getValue()))
                .sorted(Comparator.comparing(OrderBookLevel::price).reversed())
                .toList();
    }

    private List<BigDecimal> findDeletes(Set<BigDecimal> touchedPrices, List<OrderBookLevel> upserts) {
        if (touchedPrices == null || touchedPrices.isEmpty()) return List.of();

        Set<BigDecimal> upsertPrices = upserts.stream()
                .map(lv -> normalizePrice(lv.price()))
                .collect(Collectors.toSet());

        return touchedPrices.stream()
                .map(OrderBookAssembler::normalizePrice)
                .filter(p -> !upsertPrices.contains(p))
                .toList();
    }

    // 트랜잭션 커밋 후에만 diff 를 publish 한다.
    // 트랜잭션이 없으면(비동기/스케줄러 등) 즉시 publish 로 폴백.
    private void publishAfterCommit(String bunnyName, OrderBookDiff diff) {
        if (TransactionSynchronizationManager.isActualTransactionActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override public void afterCommit() {
                    orderBookPublisher.publishDiff(bunnyName, diff);
                }
            });
        } else {
            // 트랜잭션 바깥이면 그냥 즉시 전송
            orderBookPublisher.publishDiff(bunnyName, diff);
        }
    }

}
