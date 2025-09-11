package team.avgmax.rabbit.bunny.dto.data;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@Builder
public class ComparisonData {

    private String bunnyId;
    private String bunnyName;
    private String userImage;
    private int rank;
    private BigDecimal marketCap;
    private BigDecimal growthRate; // = 등락률 (현재가 - 전일 종가) / 전일 종가 * 100
}
