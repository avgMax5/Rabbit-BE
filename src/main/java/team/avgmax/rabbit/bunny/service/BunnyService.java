package team.avgmax.rabbit.bunny.service;

import com.querydsl.core.Tuple;
import lombok.extern.slf4j.Slf4j;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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
import team.avgmax.rabbit.bunny.dto.response.RabbitIndexResponse;
import team.avgmax.rabbit.bunny.entity.*;
import team.avgmax.rabbit.bunny.entity.enums.BunnyFilter;
import team.avgmax.rabbit.bunny.entity.enums.BunnyType;
import team.avgmax.rabbit.bunny.entity.enums.OrderType;
import team.avgmax.rabbit.bunny.entity.enums.ChartInterval;
import team.avgmax.rabbit.bunny.entity.enums.DeveloperType;
import team.avgmax.rabbit.bunny.exception.BunnyError;
import team.avgmax.rabbit.bunny.exception.BunnyException;
import team.avgmax.rabbit.bunny.repository.*;
import team.avgmax.rabbit.bunny.service.match.MatchingEngine;
import team.avgmax.rabbit.bunny.service.match.MatchingResult;
import team.avgmax.rabbit.bunny.service.webSocket.OrderBookAssembler;
import team.avgmax.rabbit.global.money.MoneyCalc;
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
    private final BunnyIndicatorService bunnyIndicatorService;
    private final MatchingEngine matchingEngine;

    // RABBIT 지수 조회
    @Transactional(readOnly = true)
    public RabbitIndexResponse getRabbitIndex() {
        BigDecimal baseMarketCapSum = BigDecimal.valueOf(bunnyRepository.count()).multiply(BigDecimal.valueOf(100_000_000));
        BigDecimal currentMarketCapSum = bunnyRepository.sumCurrentMarketCap();
        
        double rabbitIndex;
        if (baseMarketCapSum.compareTo(BigDecimal.ZERO) == 0) {
            rabbitIndex = 100.0;
        } else {
            rabbitIndex = currentMarketCapSum
                    .divide(baseMarketCapSum, 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100))
                    .doubleValue();
            // rabbitIndex = Math.min(rabbitIndex, 100.0); // 100을 초과하지 않도록 제한
        }
        
        return RabbitIndexResponse.builder()
                .rabbitIndex(rabbitIndex)
                .build();
    }

    // 버니 목록 조회
    @Transactional(readOnly = true)
    public Page<FetchBunnyResponse> getBunniesByFilter(BunnyFilter filter, Pageable pageable) {
        Page<Bunny> bunnies = switch (filter) {
                case ALL -> bunnyRepository.findAll(Pageable.unpaged());
                case LATEST -> bunnyRepository.findAllByOrderByCreatedAtDesc(pageable);
                case CAPITALIZATION -> bunnyRepository.findAllByOrderByMarketCapDesc(pageable);
                default -> bunnyRepository.findAll(pageable);
        };

        if (bunnies.isEmpty()) {
            log.debug("No bunnies found for filter={}", filter);
        }

        return bunnies.map(FetchBunnyResponse::from);
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
                .reliability(myBunny.getReliability())
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
        bunnyIndicatorService.updateBunnyReliability(bunny);
        bunnyIndicatorService.updateBunnyValue(bunny);
        bunnyIndicatorService.updateBunnyPopularity(bunny);
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
        bunnyIndicatorService.updateBunnyReliability(bunny);
        bunnyIndicatorService.updateBunnyValue(bunny);
        bunnyIndicatorService.updateBunnyPopularity(bunny);
    }

    private Bunny findBunnyByName(String bunnyName) {
        return bunnyRepository.findByBunnyName(bunnyName)
                .orElseThrow(() -> new BunnyException(BunnyError.BUNNY_NOT_FOUND));
    }

    // 거래 주문 요청
    @Transactional
    public OrderResponse createOrder(String bunnyName, OrderRequest request, String userId) {
        // 사용자 조회 및 잠금
        Bunny bunny = bunnyRepository.findByBunnyName(bunnyName)
                .orElseThrow(() -> new BunnyException(BunnyError.BUNNY_NOT_FOUND));
        PersonalUser user = personalUserRepository.findByIdForUpdate(userId);

        // 검증 (보유/잔액 등)
        if (request.orderType() == OrderType.SELL) {
            holdBunnyRepository.findByHolderAndBunnyForUpdate(user, bunny);
        }
        switch (request.orderType()) {
            case BUY -> validateBuy(request, user);
            case SELL -> validateSell(bunny, request, user);
            default -> throw new BunnyException(BunnyError.UNSUPPORTED_ORDER_TYPE);
        }

        // 신규 주문 저장 (초기 quantity = 요청 수량)
        Order myOrder = request.toEntity(user, bunny);
        orderRepository.save(myOrder);

        // 매수 시 예약금(원금 + 수수료) 즉시 선차감
        if (myOrder.getOrderType() == OrderType.BUY) {
            BigDecimal reserved = MoneyCalc.buyerReservation(myOrder.getQuantity(), myOrder.getUnitPrice());
            personalUserRepository.findByIdForUpdate(user.getId());
            user.subtractCarrot(reserved);
        }

        // 매도 시 매도량만큼 즉시 선차감
        if (myOrder.getOrderType() == OrderType.SELL) {
            holdBunnyRepository.adjustReservation(user.getId(), bunny.getId(), myOrder.getQuantity().negate());
        }

        // 오더북 Diff 용 터치 가격 (체결이 0건이어도 내 가격 레벨 반영)
        final Set<BigDecimal> touchedBid = new HashSet<>();
        final Set<BigDecimal> touchedAsk = new HashSet<>();
        if (myOrder.getOrderType() == OrderType.BUY) {
            touchedBid.add(myOrder.getUnitPrice());
        } else {
            touchedAsk.add(myOrder.getUnitPrice());
        }

        // 반대편 후보 조회 및 잠금
        List<Order> candidates = (myOrder.getOrderType() == OrderType.BUY)
                ? orderRepository.lockedSellCandidatesByPriceAsc(bunny.getId(), myOrder.getUnitPrice(), user.getId())
                : orderRepository.lockedBuyCandidatesByPriceDesc(bunny.getId(), myOrder.getUnitPrice(), user.getId());

        // 매칭 엔진 호출 (체결/정산/잔량처리)
        MatchingResult result = matchingEngine.match(bunny, myOrder, candidates);

        // 터치 결과
        touchedBid.addAll(result.touchedBid());
        touchedAsk.addAll(result.touchedAsk());

        // Diff 발행 (커밋 후 전송)
        emitOrderBookDiff(bunny, touchedBid, touchedAsk);

        return OrderResponse.from(myOrder);
    }

    // 거래 주문 취소
    @Transactional
    public void cancelOrder(String bunnyName, String orderId, String userId) {
        Bunny bunny = bunnyRepository.findByBunnyName(bunnyName)
                .orElseThrow(() -> new BunnyException(BunnyError.BUNNY_NOT_FOUND));

        // 대상 주문 잠금
        Order order = orderRepository.findByIdAndBunnyIdForUpdate(orderId, bunny.getId());
        if (order == null) throw new BunnyException(BunnyError.ORDER_NOT_FOUND);

        // 소유자 검증
        if (!order.getUser().getId().equals(userId)) {
            throw new BunnyException(BunnyError.FORBIDDEN);
        }

        // 이미 체결 완료라면 취소 불가
        if (order.getQuantity().signum() == 0) {
            throw new BunnyException(BunnyError.ORDER_ALREADY_FILLED);
        }

        // 매수자 취소 시 남은 잔여 예약금 환불
        if (order.getOrderType() == OrderType.BUY) {
            BigDecimal refund = MoneyCalc.buyerCancelRefund(order.getQuantity(), order.getUnitPrice());
            personalUserRepository.addCarrotForUpdate(order.getUser().getId(), refund);
        }

        // 매도자 취소시 남은 잔여 예약 수량 복원
        if (order.getOrderType() == OrderType.SELL) {
            holdBunnyRepository.adjustReservation(order.getUser().getId(), bunny.getId(), order.getQuantity());
        }

        // 주문 삭제 (취소 처리)
        orderRepository.delete(order);

        // Diff 반영할 가격 레벨 수집
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
        List<OrderBookAssembler.OrderLeaf> bidLeaves = toLeaves(buyOrders);
        List<OrderBookAssembler.OrderLeaf> askLeaves = toLeaves(sellOrders);

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
                            (devType != null) ? devType : DeveloperType.BASIC;

                    assert totalQuantity != null;
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
        BigDecimal reserved = MoneyCalc.buyerReservation(request.quantity(), request.unitPrice());
        BigDecimal currentCarrot = user.getCarrot();

        if (currentCarrot.compareTo(reserved) < 0) throw new BunnyException(BunnyError.INSUFFICIENT_BALANCE);
    }

    private void validateSell(Bunny bunny, OrderRequest request, PersonalUser user) {
        BigDecimal holding = holdBunnyRepository.findByHolderAndBunny(user, bunny)
                .map(HoldBunny::getHoldQuantity)
                .orElse(BigDecimal.ZERO);

        if (holding.compareTo(request.quantity()) < 0) throw new BunnyException(BunnyError.INSUFFICIENT_HOLDING);
    }

    private List<OrderBookAssembler.OrderLeaf> toLeaves(List<Order> orders) {
        List<OrderBookAssembler.OrderLeaf> out = new ArrayList<>(orders.size());
        for (Order order : orders) {
            if (order.getQuantity().signum() > 0) {
                out.add(new OrderBookAssembler.OrderLeaf(order.getUnitPrice(), order.getQuantity()));
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

        // 레포에서 잔여 > 0 주문만 가져옴
        List<Order> rows = orderRepository.findAllByBunnySideAndPriceIn(bunnyId, side, prices);

        Map<BigDecimal, BigDecimal> sumByPrice = rows.stream()
                .collect(Collectors.groupingBy(
                        o -> normalizePrice(o.getUnitPrice()),
                        Collectors.mapping(Order::getQuantity, Collectors.reducing(BigDecimal.ZERO, BigDecimal::add))
                ));

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
