package team.avgmax.rabbit.bunny.controller.orderBook;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Controller;
import team.avgmax.rabbit.bunny.dto.orderBook.OrderBookSnapshot;
import team.avgmax.rabbit.bunny.service.BunnyService;

@Slf4j
@Controller
@RequiredArgsConstructor
public class OrderBookWsController {

    private final BunnyService bunnyService;

    @MessageMapping("bunnies/{bunnyName}/orderbook.snapshot") // Controller 의 @RequestMapping("/bunnies") 이랑 상관없음
    @SendTo("/topic/bunnies/{bunnyName}/orderbook")
    public OrderBookSnapshot sendSnapshot(@DestinationVariable String bunnyName) {
        OrderBookSnapshot snapshot = bunnyService.getOrderBookSnapshot(bunnyName);
        log.info("WS snapshot 요청: bunnyName={}, bids={}, asks={}", bunnyName, snapshot.bids(), snapshot.asks());

        return snapshot;
    }
}
