package guru.sfg.beer.order.service.services.listener;

import com.example.common.model.event.ValidateOrderResult;
import guru.sfg.beer.order.service.config.MessagingConfig;
import guru.sfg.beer.order.service.services.BeerOrderManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.stereotype.Component;

import java.util.UUID;

import static guru.sfg.beer.order.service.config.MessagingConfig.VALIDATE_ORDER_RESPONSE_QUEUE_NAME;

@Slf4j
@Component
@RequiredArgsConstructor
public class ValidateOrderResultListener {

    private final BeerOrderManager beerOrderManager;

    @JmsListener(destination = VALIDATE_ORDER_RESPONSE_QUEUE_NAME)
    public void listen(ValidateOrderResult result) {
        UUID orderId = result.orderId();

        log.debug("Received validation result for order {}", orderId.toString());

        beerOrderManager.processValidationResult(orderId, result.isValid());
    }
}
