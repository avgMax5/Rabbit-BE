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
import team.avgmax.rabbit.bunny.controller.currentPrice.PriceTickPublisher;
import team.avgmax.rabbit.bunny.controller.orderBook.OrderBookPublisher;
import team.avgmax.rabbit.bunny.dto.currentPrice.PriceTick;
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
import team.avgmax.rabbit.bunny.dto.response.AiBunnyResponse;
import team.avgmax.rabbit.bunny.dto.response.BadgeHolderListResponse;
import team.avgmax.rabbit.bunny.dto.response.BunnyUserContextResponse;
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
import team.avgmax.rabbit.bunny.service.orderBook.OrderBookAssembler;
import team.avgmax.rabbit.global.money.MoneyCalc;
import team.avgmax.rabbit.global.util.RedisUtil;
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

import static team.avgmax.rabbit.bunny.service.orderBook.OrderBookAssembler.normalizePrice;

import team.avgmax.rabbit.ai.service.ChatClientService;

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
    private final CorporationUserRepository corporationUserRepository;
    private final OrderRepository orderRepository;
    private final MatchRepository matchRepository;
    private final OrderBookAssembler orderBookAssembler;
    private final OrderBookPublisher orderBookPublisher;
    private final PriceTickPublisher priceTickPublisher;
    private final ChatClientService chatClientService;
    private final MatchingEngine matchingEngine;
    private final RedisUtil redisUtil;

    private static final String LIKE_SET_KEY_PREFIX = "bunny_like:";

    // RABBIT 지수 조회
    @Transactional(readOnly = true)
    public RabbitIndexResponse getRabbitIndex() {
        BigDecimal baseMarketCapSum = BigDecimal.valueOf(bunnyRepository.count()).multiply(BigDecimal.valueOf(100_000_000));
        BigDecimal currentMarketCapSum = bunnyRepository.sumCurrentMarketCap();
        
        double rabbitIndex;
        if (baseMarketCapSum.compareTo(BigDecimal.ZERO) == 0) {
            rabbitIndex = 100.0;
        } else {
            double rawIndex = currentMarketCapSum
                    .divide(baseMarketCapSum, 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100))
                    .doubleValue();
            rabbitIndex = Math.min(rawIndex, 200.0);
        }
        double finalScore = rabbitIndex / 2.0;
        
        return RabbitIndexResponse.builder()
                .rabbitIndex(finalScore)
                .build();
    }

    // 업데이트 알림 목록 조회
    @Transactional(readOnly = true)
    public List<String> getUpdateAlerts() {
        return bunnyRepository.findTop10ByOrderBySpecUpdatedAtDesc().stream()
                .map(Bunny::getBunnyName)
                .toList();
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

        // Redis에서 실시간 좋아요 수 조회
        long realTimeLikeCount = getTotalLikeCount(bunny.getId());
        return FetchBunnyResponse.from(bunny, realTimeLikeCount);
    }

    // 버니 사용자 컨텍스트 조회 (매수/매도 가능한 금액과 수량)
    @Transactional(readOnly = true)
    public BunnyUserContextResponse getBunnyUserContext(String bunnyName, String userId) {
        Bunny bunny = bunnyRepository.findByBunnyName(bunnyName)
                .orElseThrow(() -> new BunnyException(BunnyError.BUNNY_NOT_FOUND));
        
        // 사용자 정보 조회
        PersonalUser user = personalUserRepository.findById(userId)
                .orElseThrow(() -> new UserException(UserError.USER_NOT_FOUND));

        // Redis와 DB 모두 확인하여 좋아요 여부 판단
        String likeSetKey = LIKE_SET_KEY_PREFIX + bunny.getId();
        boolean isLikedInRedis = redisUtil.isMemberOfSet(likeSetKey, userId);
        boolean isLikedInDb = bunnyLikeRepository.existsByBunnyIdAndUserId(bunny.getId(), userId);
        boolean isLiked = isLikedInRedis || isLikedInDb;

        // 매도 가능한 수량 계산 (사용자가 보유한 해당 bunny의 quantity 합계)
        BigDecimal sellableQuantity = holdBunnyRepository.findTotalQuantityByUserIdAndBunnyId(userId, bunny.getId());

        // 매수 가능한 금액 계산
        BigDecimal buyableAmount = calculateBuyableAmount(user, bunny);

        return BunnyUserContextResponse.of(
                isLiked,
                buyableAmount,
                sellableQuantity
        );
    }

    // 마이 버니 조회
    @Transactional(readOnly = true)
    public MyBunnyResponse getMyBunny(String userId) {
        // 사용자 조회
        PersonalUser personalUser = personalUserRepository.findById(userId)
                .orElseThrow(() -> new UserException(UserError.USER_NOT_FOUND));
        Bunny myBunny = bunnyRepository.findByUserId(personalUser.getId())
                .orElseThrow(() -> new BunnyException(BunnyError.BUNNY_NOT_FOUND));

        // 필요 데이터 수집 (쿼리/계산)
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

        return OrderListResponse.from(ordersResponse);
    }

    // 좋아요 추가
    public void addBunnyLike(String bunnyName, String userId, Role role) {
        Bunny bunny = findBunnyByName(bunnyName);
        String likeSetKey = LIKE_SET_KEY_PREFIX + bunny.getId();
        
        // Redis Set에 추가 (중복 자동 방지)
        redisUtil.addToSet(likeSetKey, userId);
        
        // Corporation 사용자의 경우 Badge는 즉시 처리 (비즈니스 로직상 중요)
        if (role == Role.ROLE_CORPORATION) {
            CorporationUser corporationUser = corporationUserRepository.findById(userId)
                .orElseThrow(() -> new UserException(UserError.USER_NOT_FOUND));
            badgeRepository.save(Badge.create(bunny.getId(), userId, corporationUser.getCorporationName()));
        }
    }

    // 좋아요 취소
    public void cancelBunnyLike(String bunnyName, String userId, Role role) {
        Bunny bunny = findBunnyByName(bunnyName);
        String likeSetKey = LIKE_SET_KEY_PREFIX + bunny.getId();
        
        // Redis Set에서 제거
        redisUtil.removeFromSet(likeSetKey, userId);
        
        // Corporation 사용자의 경우 Badge는 즉시 삭제 (비즈니스 로직상 중요)
        if (role == Role.ROLE_CORPORATION) {
            badgeRepository.deleteByBunnyIdAndUserId(bunny.getId(), userId);
        }
    }

    private Bunny findBunnyByName(String bunnyName) {
        return bunnyRepository.findByBunnyName(bunnyName)
                .orElseThrow(() -> new BunnyException(BunnyError.BUNNY_NOT_FOUND));
    }

    /**
     * Redis Set의 크기를 실시간 좋아요 수로 반환
     * DB의 likeCount는 스케줄러로 주기적으로 동기화됨
     */
    private long getTotalLikeCount(String bunnyId) {
        String likeSetKey = LIKE_SET_KEY_PREFIX + bunnyId;
        long redisLikeCount = redisUtil.getSetSize(likeSetKey);
        
        // Redis에 데이터가 없으면 DB 값 사용 (초기 상태 또는 Redis 장애 시)
        if (redisLikeCount == 0) {
            Bunny bunny = bunnyRepository.findById(bunnyId)
                    .orElseThrow(() -> new BunnyException(BunnyError.BUNNY_NOT_FOUND));
            return bunny.getLikeCount();
        }
        
        return redisLikeCount;
    }

    // 거래 주문 요청
    @Transactional
    public OrderResponse createOrder(String bunnyName, OrderRequest request, String userId) {
        // 사용자 조회 및 잠금
        Bunny bunny = bunnyRepository.findByBunnyName(bunnyName)
                .orElseThrow(() -> new BunnyException(BunnyError.BUNNY_NOT_FOUND));
        PersonalUser user = personalUserRepository.findByIdForUpdate(userId);

        switch (request.orderType()) {
            case BUY -> validateBuy(request, user);
            case SELL -> validateSell(bunny, request, user);
            default -> throw new BunnyException(BunnyError.UNSUPPORTED_ORDER_TYPE);
        }

        // 신규 주문 저장 (초기 quantity = 요청 수량)
        Order myOrder = orderRepository.save(request.toEntity(user, bunny));

        // 매수 시 예약금(원금 + 수수료) 즉시 선차감
        if (myOrder.getOrderType() == OrderType.BUY) {
            BigDecimal reserved = MoneyCalc.buyerReservation(myOrder.getQuantity(), myOrder.getUnitPrice());
            personalUserRepository.findByIdForUpdate(user.getId());
            user.subtractCarrot(reserved);
        }

        // 매도 시 매도량만큼 즉시 선차감
        if (myOrder.getOrderType() == OrderType.SELL) {
            HoldBunny holdBunny = holdBunnyRepository.findByHolderAndBunny(user, bunny)
                    .orElseThrow(() -> new BunnyException(BunnyError.HOLD_BUNNY_NOT_FOUND));
            holdBunny.preDecreaseQuantity(myOrder.getQuantity());
            // holdBunnyRepository.adjustReservation(user.getId(), bunny.getId(), myOrder.getQuantity().negate());
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

        // OrderBookDiff 발행 (커밋 후 전송)
        emitOrderBookDiff(bunny, touchedBid, touchedAsk);

        if (result.lastTradePrice() != null) {
            publishAfterCommitForPrice(bunny.getBunnyName(), result.lastTradePrice());
        }

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
            PersonalUser user = personalUserRepository.findByIdForUpdate(order.getUser().getId());
            user.addCarrot(refund);
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
        List<OrderBookAssembler.OrderLeaf> allLeaves = new ArrayList<>();
        allLeaves.addAll(toLeaves(buyOrders));
        allLeaves.addAll(toLeaves(sellOrders));

        List<OrderBookLevel> orders = orderBookAssembler.toLevel(allLeaves);

        BigDecimal currentPrice = queryCurrentPrice(bunny);

        return OrderBookSnapshot.from(bunny, orders, currentPrice);
    }

    // AI 응답 동기화
    @Transactional
    public AiBunnyResponse syncAiResponse(String bunnyName) {
        Bunny bunny = bunnyRepository.findByBunnyName(bunnyName)
                .orElseThrow(() -> new BunnyException(BunnyError.BUNNY_NOT_FOUND));
        String aiReview = chatClientService.getAiReviewOfBunny(bunny);
        String aiFeedback = chatClientService.getAiFeedbackOfBunny(bunny);
        bunny.updateAiReviewAndFeedback(aiReview, aiFeedback);
        return AiBunnyResponse.from(bunny);
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

    @Transactional(readOnly = true)
    public BadgeHolderListResponse getBadgeHolders(String badgeName) {
        String badgeImg = badgeName.toUpperCase();
        List<Bunny> bunnies = bunnyRepository.findAllByBadgeImg(badgeImg);
        return BadgeHolderListResponse.from(badgeImg, bunnies);
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
                .map(tuple -> toDevTypeData(tuple, totalSupply)) // Optional<MyBunnyByDevTypeData>
                .flatMap(Optional::stream)
                .toList();
    }

    private Optional<MyBunnyByDevTypeData> toDevTypeData(Tuple tuple, BigDecimal totalSupply) {
        BigDecimal totalQuantity = tuple.get(1, BigDecimal.class);
        if (totalQuantity == null) return Optional.empty();

        DeveloperType type = Optional.ofNullable(tuple.get(0, DeveloperType.class))
                .orElse(DeveloperType.BASIC);
        long count = Optional.ofNullable(tuple.get(2, Long.class)).orElse(0L);

        // (totalQuantity / totalSupply) * 100, 소수 4자리 반올림
        BigDecimal percentage = totalQuantity
                .divide(totalSupply, 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100));

        return Optional.of(
                MyBunnyByDevTypeData.builder()
                        .developerType(type)
                        .percentage(percentage)
                        .count(count)
                        .build()
        );
    }

    private List<MyBunnyByHolderData> getHolders(String bunnyId) {
        return holdBunnyRepository.findHoldersByBunnyId(bunnyId);
    }

    private List<DailyPriceData> getMonthlyGrowthRate(String bunnyId) {
        List<BunnyHistory> histories = bunnyHistoryRepository.findAllByBunnyIdOrderByDateAsc(bunnyId);
        if (histories.isEmpty()) return Collections.emptyList();

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
        if (prev == null || prev.compareTo(BigDecimal.ZERO) == 0 || current == null) return null;
        return current.subtract(prev)
                .divide(prev, 6, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100))
                .setScale(2, RoundingMode.HALF_UP);
    }

    private void validateBuy(OrderRequest request, PersonalUser user) {
        if (request.quantity() == null || request.quantity().signum() <= 0) {
            throw new BunnyException(BunnyError.INVALID_QUANTITY);
        }

        if (request.unitPrice() == null || request.unitPrice().signum() <= 0) {
            throw new BunnyException(BunnyError.INVALID_PRICE);
        }

        // 총 예약금
        BigDecimal reserved = MoneyCalc.buyerReservation(request.quantity(), request.unitPrice());

        // 유저의 현재 보유 캐럿
        BigDecimal currentCarrot = user.getCarrot();
        if (currentCarrot == null || currentCarrot.signum() <= 0) throw new BunnyException(BunnyError.INSUFFICIENT_BALANCE);
        if (currentCarrot.compareTo(reserved) < 0) throw new BunnyException(BunnyError.INSUFFICIENT_BALANCE);
    }

    private void validateSell(Bunny bunny, OrderRequest request, PersonalUser user) {
        if (request.quantity() == null || request.quantity().signum() <= 0) {
            throw new BunnyException(BunnyError.INVALID_QUANTITY);
        }

        BigDecimal holding = holdBunnyRepository.findByHolderAndBunnyForUpdate(user, bunny)
                .map(HoldBunny::getHoldQuantity)
                .orElse(BigDecimal.ZERO);

        if (holding.compareTo(request.quantity()) < 0) throw new BunnyException(BunnyError.INSUFFICIENT_HOLDING);
    }

    private List<OrderBookAssembler.OrderLeaf> toLeaves(List<Order> orders) {
        List<OrderBookAssembler.OrderLeaf> out = new ArrayList<>(orders.size());
        for (Order order : orders) {
            if (order.getQuantity().signum() > 0) {
                out.add(new OrderBookAssembler.OrderLeaf(order.getUnitPrice(), order.getQuantity(), order.getOrderType()));
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
        if ((bidPrices == null || bidPrices.isEmpty()) && (askPrices == null || askPrices.isEmpty())) return;

        // 부분 집계들 (Upserts/Deletes)
        Set<BigDecimal> allTouchedPrices = new HashSet<>();
        if (bidPrices != null) allTouchedPrices.addAll(bidPrices);
        if (askPrices != null) allTouchedPrices.addAll(askPrices);
        
        // 모든 주문 타입에 대해 집계
        List<OrderBookLevel> orderUpserts = new ArrayList<>();
        for (OrderType type : List.of(OrderType.BUY, OrderType.SELL)) {
            Set<BigDecimal> typePrices = (type == OrderType.BUY) ? bidPrices : askPrices;
            if (typePrices != null && !typePrices.isEmpty()) {
                orderUpserts.addAll(aggregateLevelsForPrices(bunny.getId(), type, typePrices));
            }
        }

        // 잔여가 0이면 → delete
        List<BigDecimal> orderDeletes = new ArrayList<>();
        Set<BigDecimal> upsertPrices = orderUpserts.stream()
                .map(lv -> normalizePrice(lv.price()))
                .collect(Collectors.toSet());
        
        orderDeletes.addAll(allTouchedPrices.stream()
                .map(OrderBookAssembler::normalizePrice)
                .filter(p -> !upsertPrices.contains(p))
                .toList());

        BigDecimal currentPrice = queryCurrentPrice(bunny);

        OrderBookDiff diff = new OrderBookDiff(
                bunny.getBunnyName(),
                orderUpserts,
                orderDeletes,
                currentPrice,
                System.currentTimeMillis()
        );

        // 트랜잭션 커밋 이후에만 브로드캐스트
        publishAfterCommitForOrderBook(bunny.getBunnyName(), diff);
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
                .map(e -> new OrderBookLevel(e.getKey(), e.getValue(), side))
                .sorted(Comparator.comparing(OrderBookLevel::price).reversed())
                .toList();
    }


    // 트랜잭션 커밋 후에만 diff 를 publish 한다.
    // 트랜잭션이 없으면(비동기/스케줄러 등) 즉시 publish 로 폴백.
    private void publishAfterCommitForOrderBook(String bunnyName, OrderBookDiff diff) {
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

    private void publishAfterCommitForPrice(String bunnyName, BigDecimal currentPrice) {
        if (TransactionSynchronizationManager.isActualTransactionActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    priceTickPublisher.publishTick(new PriceTick(bunnyName, currentPrice, System.currentTimeMillis()));
                }
            });
        } else {
            priceTickPublisher.publishTick(new PriceTick(bunnyName, currentPrice, System.currentTimeMillis()));
        }
    }

    private BigDecimal calculateBuyableAmount(PersonalUser user, Bunny bunny) {
        // 사용자의 현재 캐럿 보유량
        BigDecimal userCarrot = user.getCarrot();
        
        // bunny 타입별 총 공급량
        BigDecimal totalSupply = bunny.getBunnyType().getTotalSupply();
        
        // 사용자가 최대 보유 가능한 수량 (총 공급량의 50%)
        BigDecimal maxHoldableQuantity = totalSupply.multiply(BigDecimal.valueOf(0.5));
        
        // 사용자가 현재 보유한 수량
        BigDecimal currentHoldQuantity = holdBunnyRepository.findTotalQuantityByUserIdAndBunnyId(user.getId(), bunny.getId());
        
        // 추가로 매수 가능한 수량
        BigDecimal additionalBuyableQuantity = maxHoldableQuantity.subtract(currentHoldQuantity);
        
        // 추가 매수가 불가능한 경우 (이미 50% 이상 보유)
        if (additionalBuyableQuantity.compareTo(BigDecimal.ZERO) <= 0) return BigDecimal.ZERO;
        
        // 현재가 기준으로 추가 매수 가능한 금액
        BigDecimal maxBuyableAmountByQuantity = bunny.getCurrentPrice().multiply(additionalBuyableQuantity);
        
        // 사용자 캐럿과 수량 제한 중 더 작은 값이 실제 매수 가능한 금액
        return userCarrot.min(maxBuyableAmountByQuantity);
    }
}
