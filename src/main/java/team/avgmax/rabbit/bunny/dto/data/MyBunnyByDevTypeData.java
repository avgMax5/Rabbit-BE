package team.avgmax.rabbit.bunny.dto.data;

import lombok.Builder;
import lombok.Getter;
import team.avgmax.rabbit.bunny.entity.enums.DeveloperType;

import java.math.BigDecimal;

@Getter
@Builder
public class MyBunnyByDevTypeData {

    private DeveloperType developerType;
    private BigDecimal percentage;
    private long count;
}
