package team.avgmax.rabbit.bunny.dto.data;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@Builder
public class MyBunnyByHolderData {

    private String userId;
    private String userName;
    private String userImg;
    private BigDecimal holdQuantity;
}
