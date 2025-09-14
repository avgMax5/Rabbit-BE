package team.avgmax.rabbit.bunny.dto.response;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.Builder;
import lombok.Getter;
import team.avgmax.rabbit.bunny.entity.Bunny;
import team.avgmax.rabbit.bunny.entity.enums.ChartInterval;

import java.util.List;

@Getter
@Builder
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class ChartResponse {

    private String bunnyName; // 혹시 몰라서 추가 해놓은 필드, 삭제 가능
    private ChartInterval interval; // DAILY, WEEKLY, MONTHLY
    private List<ChartDataPoint> chartDataList;

    public static ChartResponse from(List<ChartDataPoint> chartDataList, Bunny bunny, ChartInterval interval) {
        return ChartResponse.builder()
                .bunnyName(bunny.getBunnyName())
                .interval(interval)
                .chartDataList(chartDataList)
                .build();
    }
    public static ChartResponse from(List<ChartDataPoint> chartDataList, String bunnyName, ChartInterval interval) {
        return ChartResponse.builder()
                .bunnyName(bunnyName)
                .interval(interval)
                .chartDataList(chartDataList)
                .build();
    }
}
