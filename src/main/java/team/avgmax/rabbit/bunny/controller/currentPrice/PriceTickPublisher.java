package team.avgmax.rabbit.bunny.controller.currentPrice;

import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import team.avgmax.rabbit.bunny.dto.currentPrice.ClosingPriceUpdate;
import team.avgmax.rabbit.bunny.dto.currentPrice.PriceTick;

@Component
@RequiredArgsConstructor
public class PriceTickPublisher {

    private final SimpMessagingTemplate messaging;

    public void publishTick(PriceTick tick) {
        String destination = "/topic/price/" + tick.bunnyName();
        messaging.convertAndSend(destination, tick);
    }

    public void publishClose(ClosingPriceUpdate close) {
        String destination = "/topic/close/" + close.bunnyName();
        messaging.convertAndSend(destination, close);
    }

    // currentPrice에 관련된 Snapshot은 REST API로 대체 가능하기 때문에 Controller를 따로 만들지 않았음.
}
