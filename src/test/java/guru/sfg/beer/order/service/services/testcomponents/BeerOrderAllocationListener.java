package guru.sfg.beer.order.service.services.testcomponents;

import com.example.common.model.BeerOrderDto;
import com.example.common.model.event.AllocateOrderRequest;
import com.example.common.model.event.AllocateOrderResult;
import com.example.common.model.event.ValidateOrderRequest;
import com.example.common.model.event.ValidateOrderResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.stereotype.Component;

import java.util.UUID;

import static guru.sfg.beer.order.service.config.MessagingConfig.ALLOCATE_ORDER_QUEUE_NAME;
import static guru.sfg.beer.order.service.config.MessagingConfig.ALLOCATE_ORDER_RESPONSE_QUEUE_NAME;
import static guru.sfg.beer.order.service.config.MessagingConfig.VALIDATE_ORDER_QUEUE_NAME;
import static guru.sfg.beer.order.service.config.MessagingConfig.VALIDATE_ORDER_RESPONSE_QUEUE_NAME;

@Slf4j
@Component
@RequiredArgsConstructor
public class BeerOrderAllocationListener {

    private final JmsTemplate jmsTemplate;

    @JmsListener(destination = ALLOCATE_ORDER_QUEUE_NAME)
    public void listen(AllocateOrderRequest request) {
        BeerOrderDto beerOrder = request.beerOrder();

        log.debug("Received allocation request for order {}", beerOrder.getId().toString());

        beerOrder.getBeerOrderLines().forEach(beerOrderLineDto -> {
            beerOrderLineDto.setQuantityAllocated(beerOrderLineDto.getOrderQuantity());
        });

        jmsTemplate.convertAndSend(ALLOCATE_ORDER_RESPONSE_QUEUE_NAME,
                AllocateOrderResult.builder().beerOrder(beerOrder).build());
    }
}
