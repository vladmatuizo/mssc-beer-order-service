package guru.sfg.beer.order.service.statemachine.action;

import com.example.common.model.event.AllocationFailureEvent;
import guru.sfg.beer.order.service.domain.BeerOrderEventEnum;
import guru.sfg.beer.order.service.domain.BeerOrderStatusEnum;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.statemachine.StateContext;
import org.springframework.statemachine.action.Action;
import org.springframework.stereotype.Component;

import java.util.UUID;

import static guru.sfg.beer.order.service.config.MessagingConfig.ALLOCATION_FAILURE_QUEUE_NAME;
import static guru.sfg.beer.order.service.services.StateMachineBeerOrderManager.BEER_ORDER_ID_HEADER;

@Slf4j
@Component
@RequiredArgsConstructor
public class AllocationFailureAction implements Action<BeerOrderStatusEnum, BeerOrderEventEnum> {

    private final JmsTemplate jmsTemplate;

    @Override
    public void execute(StateContext<BeerOrderStatusEnum, BeerOrderEventEnum> stateContext) {
        String beerOrderId = (String) stateContext.getMessageHeader(BEER_ORDER_ID_HEADER);

        jmsTemplate.convertAndSend(ALLOCATION_FAILURE_QUEUE_NAME,
                new AllocationFailureEvent(UUID.fromString(beerOrderId))
        );

        log.debug("Sent allocation failure event to queue {} for order with id {}",
                ALLOCATION_FAILURE_QUEUE_NAME, beerOrderId);
    }
}
