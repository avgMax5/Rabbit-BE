package team.avgmax.rabbit.user.repository.custom;

import com.querydsl.core.Tuple;
import team.avgmax.rabbit.bunny.dto.data.MyBunnyByHolderData;
import team.avgmax.rabbit.user.dto.response.HoldBunniesResponse;

import java.util.List;

public interface HoldBunnyRepositoryCustom {
    HoldBunniesResponse findHoldBunniesByUserId(String personalUserId);

    List<MyBunnyByHolderData> findHoldersByBunnyId(String bunnyId);

    List<Tuple> findHolderTypeDistributionByBunnyId(String bunnyId);
}