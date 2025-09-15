package team.avgmax.rabbit.bunny.dto.data;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@Builder
@AllArgsConstructor
public class MyBunnyByHolderData {

    private String userId;
    private String userName;
    private String userImg;
    private BigDecimal holdQuantity;
}
