package team.avgmax.rabbit.user.repository.custom;

import com.querydsl.core.Tuple;
import team.avgmax.rabbit.bunny.dto.data.MyBunnyByHolderData;
import java.math.BigDecimal;
import java.util.List;

public interface HoldBunnyRepositoryCustom {
    List<MyBunnyByHolderData> findHoldersByBunnyId(String bunnyId);

    List<Tuple> findHolderTypeDistributionByBunnyId(String bunnyId);

    void applyBuyMatch(String userId, String bunnyId, BigDecimal qty, BigDecimal tradeBaseAmount);

    void applySellMatch(String userId, String bunnyId, BigDecimal filledQty);

    void adjustReservation(String userId, String bunnyId, BigDecimal deltaQty);

    void deleteIfEmpty(String userId, String bunnyId);

    BigDecimal findTotalQuantityByUserIdAndBunnyId(String userId, String bunnyId);
}