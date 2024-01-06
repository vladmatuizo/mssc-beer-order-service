package guru.sfg.beer.order.service.services.testcomponents;

import com.example.common.model.event.ValidateOrderRequest;
import com.example.common.model.event.ValidateOrderResult;
import guru.sfg.beer.order.service.config.MessagingConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.stereotype.Component;

import java.util.UUID;

import static guru.sfg.beer.order.service.config.MessagingConfig.VALIDATE_ORDER_QUEUE_NAME;
import static guru.sfg.beer.order.service.config.MessagingConfig.VALIDATE_ORDER_RESPONSE_QUEUE_NAME;

@Slf4j
@Component
@RequiredArgsConstructor
public class BeerOrderValidationListener {

    private final JmsTemplate jmsTemplate;

    @JmsListener(destination = VALIDATE_ORDER_QUEUE_NAME)
    public void listen(ValidateOrderRequest request) {
        UUID orderId = request.beerOrder().getId();

        log.debug("Received validation request for order {}", orderId.toString());

        jmsTemplate.convertAndSend(VALIDATE_ORDER_RESPONSE_QUEUE_NAME,
                new ValidateOrderResult(orderId, true));
    }
}
