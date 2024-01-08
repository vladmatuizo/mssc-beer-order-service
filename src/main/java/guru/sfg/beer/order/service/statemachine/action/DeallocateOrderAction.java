package guru.sfg.beer.order.service.statemachine.action;

import com.example.common.model.event.DeallocateOrderRequest;
import guru.sfg.beer.order.service.domain.BeerOrder;
import guru.sfg.beer.order.service.domain.BeerOrderEventEnum;
import guru.sfg.beer.order.service.domain.BeerOrderStatusEnum;
import guru.sfg.beer.order.service.repositories.BeerOrderRepository;
import guru.sfg.beer.order.service.web.mappers.BeerOrderMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.statemachine.StateContext;
import org.springframework.statemachine.action.Action;
import org.springframework.stereotype.Component;

import java.util.UUID;

import static guru.sfg.beer.order.service.config.MessagingConfig.DEALLOCATE_ORDER_QUEUE_NAME;
import static guru.sfg.beer.order.service.services.StateMachineBeerOrderManager.BEER_ORDER_ID_HEADER;

@Slf4j
@Component
@RequiredArgsConstructor
public class DeallocateOrderAction implements Action<BeerOrderStatusEnum, BeerOrderEventEnum> {

    private final BeerOrderRepository beerOrderRepository;
    private final BeerOrderMapper beerOrderMapper;
    private final JmsTemplate jmsTemplate;

    @Override
    public void execute(StateContext<BeerOrderStatusEnum, BeerOrderEventEnum> stateContext) {
        String beerOrderId = (String) stateContext.getMessageHeader(BEER_ORDER_ID_HEADER);
        BeerOrder beerOrder = beerOrderRepository.findById(UUID.fromString(beerOrderId)).orElseThrow();

        jmsTemplate.convertAndSend(DEALLOCATE_ORDER_QUEUE_NAME,
                new DeallocateOrderRequest(beerOrderMapper.beerOrderToDto(beerOrder))
        );

        log.debug("Sent deallocation request to queue {} for order with id {}", DEALLOCATE_ORDER_QUEUE_NAME, beerOrderId);
    }
}
