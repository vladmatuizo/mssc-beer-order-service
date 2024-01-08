package guru.sfg.beer.order.service.services.listener;

import com.example.common.model.BeerOrderDto;
import com.example.common.model.event.AllocateOrderResult;
import guru.sfg.beer.order.service.services.BeerOrderManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.stereotype.Component;

import static guru.sfg.beer.order.service.config.MessagingConfig.ALLOCATE_ORDER_RESPONSE_QUEUE_NAME;

@Slf4j
@Component
@RequiredArgsConstructor
public class AllocationResultListener {

    private final BeerOrderManager beerOrderManager;

    @JmsListener(destination = ALLOCATE_ORDER_RESPONSE_QUEUE_NAME)
    public void listen(AllocateOrderResult allocateOrderResult) {
        BeerOrderDto beerOrder = allocateOrderResult.getBeerOrder();

        if (!allocateOrderResult.isAllocationError() && !allocateOrderResult.isPendingInventory()) {
            beerOrderManager.processSuccessfulBeerOrderAllocation(beerOrder);
        } else if (!allocateOrderResult.isAllocationError()) {
            beerOrderManager.processBeerOrderAllocationPendingInventory(beerOrder);
        } else {
            beerOrderManager.processBeerOrderAllocationFailure(beerOrder);
        }
    }
}
