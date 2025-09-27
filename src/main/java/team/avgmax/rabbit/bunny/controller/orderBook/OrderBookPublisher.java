package team.avgmax.rabbit.bunny.controller.orderBook;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import team.avgmax.rabbit.bunny.dto.orderBook.OrderBookDiff;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderBookPublisher {

    private final SimpMessagingTemplate messaging;

    public void publishDiff(String bunnyName, OrderBookDiff diff) {
        String destination = "/topic/bunnies/" + bunnyName + "/orderbook";
        log.info(
                "호가 diff 전송: destination={}, bidUpserts={}, bidDeletes={}, askUpserts={}, askDeletes={}, currentPrice={}",
                destination,
                diff.bidUpserts().size(),
                diff.bidDeletes().size(),
                diff.askUpserts().size(),
                diff.askDeletes().size(),
                diff.currentPrice()
        );

        messaging.convertAndSend(destination, diff);
    }
}
