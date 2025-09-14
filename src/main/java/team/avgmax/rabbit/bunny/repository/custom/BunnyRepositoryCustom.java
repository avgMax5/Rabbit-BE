package team.avgmax.rabbit.bunny.repository.custom;

import team.avgmax.rabbit.bunny.entity.Bunny;
import team.avgmax.rabbit.bunny.entity.enums.BunnyType;
import team.avgmax.rabbit.bunny.entity.enums.DeveloperType;
import team.avgmax.rabbit.user.entity.enums.Position;

import java.math.BigDecimal;
import java.util.List;

public interface BunnyRepositoryCustom {

    // 로켓 탑승한 버니들 (배지 갯수 3개 or 0~2개의 2가지의 그룹으로 나누어 정렬하고, 각각 오래된 생성일 순으로 정렬)
    List<Bunny> findAllByPriorityGroupAndCreatedAt();

    BigDecimal findAverageGrowthRateByBunnyType(BunnyType bunnyType);

    BigDecimal findAverageGrowthRateByPosition(Position position);

    BigDecimal findAverageGrowthRateByDeveloperType(DeveloperType developerType);

    List<Bunny> findAllWithUserOrderByMarketCapDesc();

}
