package guru.sfg.beer.order.service.services;

import com.example.common.model.BeerOrderDto;
import guru.sfg.beer.order.service.domain.BeerOrder;
import guru.sfg.beer.order.service.domain.BeerOrderEventEnum;
import guru.sfg.beer.order.service.domain.BeerOrderStatusEnum;
import guru.sfg.beer.order.service.repositories.BeerOrderRepository;
import guru.sfg.beer.order.service.statemachine.BeerOrderStateChangeInterceptor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.statemachine.StateMachine;
import org.springframework.statemachine.config.StateMachineFactory;
import org.springframework.statemachine.support.DefaultStateMachineContext;
import org.springframework.stereotype.Service;

import java.util.UUID;

import static guru.sfg.beer.order.service.domain.BeerOrderEventEnum.ALLOCATE_ORDER;
import static guru.sfg.beer.order.service.domain.BeerOrderEventEnum.ALLOCATION_FAILED;
import static guru.sfg.beer.order.service.domain.BeerOrderEventEnum.ALLOCATION_NO_INVENTORY;
import static guru.sfg.beer.order.service.domain.BeerOrderEventEnum.ALLOCATION_SUCCESS;
import static guru.sfg.beer.order.service.domain.BeerOrderEventEnum.PICK_UP;
import static guru.sfg.beer.order.service.domain.BeerOrderEventEnum.VALIDATE_ORDER;
import static guru.sfg.beer.order.service.domain.BeerOrderEventEnum.VALIDATION_FAILED;
import static guru.sfg.beer.order.service.domain.BeerOrderEventEnum.VALIDATION_PASSED;
import static guru.sfg.beer.order.service.domain.BeerOrderStatusEnum.NEW;

@Slf4j
@Service
@RequiredArgsConstructor
public class StateMachineBeerOrderManager implements BeerOrderManager {

    public static final String BEER_ORDER_ID_HEADER = "beer_order_id";

    private final StateMachineFactory<BeerOrderStatusEnum, BeerOrderEventEnum> stateMachineFactory;
    private final BeerOrderStateChangeInterceptor beerOrderStateChangeInterceptor;

    private final BeerOrderRepository beerOrderRepository;

    @Override
    public BeerOrder create(BeerOrder beerOrder) {
        beerOrder.setId(null);
        beerOrder.setOrderStatus(NEW);

        BeerOrder savedOrder = beerOrderRepository.save(beerOrder);
        sendBeerOrderEvent(savedOrder, VALIDATE_ORDER);
        return savedOrder;
    }

    @Override
    public void processValidationResult(UUID orderId, boolean isValid) {
        BeerOrder beerOrder = beerOrderRepository.findById(orderId).orElseThrow();

        if (isValid) {
            log.debug("Validation passed for order {}, sending VALIDATION_PASSED event to state machine", orderId);
            sendBeerOrderEvent(beerOrder, VALIDATION_PASSED);

            BeerOrder validatedOrder = beerOrderRepository.findById(orderId).orElseThrow();
            log.debug("Valid order state {}, sending ALLOCATE_ORDER event to state machine", validatedOrder.getOrderStatus());

            sendBeerOrderEvent(validatedOrder, ALLOCATE_ORDER);
            log.debug("Event ALLOCATE_ORDER is sent");
        } else {
            sendBeerOrderEvent(beerOrder, VALIDATION_FAILED);
            log.debug("Event VALIDATION_FAILED is sent");
        }
    }

    @Override
    public void processSuccessfulBeerOrderAllocation(BeerOrderDto beerOrder) {
        BeerOrder allocatedOrder = beerOrderRepository.findById(beerOrder.getId()).orElseThrow();
        sendBeerOrderEvent(allocatedOrder, ALLOCATION_SUCCESS);

        updateAllocatedQuantity(beerOrder);
    }

    @Override
    public void processBeerOrderAllocationPendingInventory(BeerOrderDto beerOrder) {
        BeerOrder allocatedOrder = beerOrderRepository.findById(beerOrder.getId()).orElseThrow();
        sendBeerOrderEvent(allocatedOrder, ALLOCATION_NO_INVENTORY);

        updateAllocatedQuantity(beerOrder);
    }

    private void updateAllocatedQuantity(BeerOrderDto beerOrderDto) {
        BeerOrder allocatedOrder = beerOrderRepository.findById(beerOrderDto.getId()).orElseThrow();

        allocatedOrder.getBeerOrderLines().forEach(beerOrderLine ->
                beerOrderDto.getBeerOrderLines().forEach(beerOrderLineDto -> {
                    if (beerOrderLine.getId().equals(beerOrderLineDto.getId())) {
                        beerOrderLine.setQuantityAllocated(beerOrderLineDto.getQuantityAllocated());
                    }
                }));

        beerOrderRepository.saveAndFlush(allocatedOrder);
    }

    @Override
    public void processBeerOrderAllocationFailure(BeerOrderDto beerOrder) {
        BeerOrder failedOrder = beerOrderRepository.findById(beerOrder.getId()).orElseThrow();
        sendBeerOrderEvent(failedOrder, ALLOCATION_FAILED);
    }

    @Override
    public void processBeerOrderPickUp(UUID beerId) {
        BeerOrder beerOrder = beerOrderRepository.findById(beerId).orElseThrow();
        sendBeerOrderEvent(beerOrder, PICK_UP);
    }

    private void sendBeerOrderEvent(BeerOrder beerOrder, BeerOrderEventEnum beerOrderEvent) {
        StateMachine<BeerOrderStatusEnum, BeerOrderEventEnum> stateMachine = buildStateMachine(beerOrder);

        Message<BeerOrderEventEnum> message = MessageBuilder.withPayload(beerOrderEvent)
                .setHeader(BEER_ORDER_ID_HEADER, beerOrder.getId().toString())
                .build();

        stateMachine.sendEvent(message);
    }

    private StateMachine<BeerOrderStatusEnum, BeerOrderEventEnum> buildStateMachine(BeerOrder beerOrder) {
        StateMachine<BeerOrderStatusEnum, BeerOrderEventEnum> stateMachine = stateMachineFactory.getStateMachine(beerOrder.getId());

        stateMachine.stop();

        stateMachine.getStateMachineAccessor()
                .doWithAllRegions(access -> {
                    access.addStateMachineInterceptor(beerOrderStateChangeInterceptor);
                    access.resetStateMachine(new DefaultStateMachineContext<>(beerOrder.getOrderStatus(), null, null, null));
                });

        stateMachine.start();
        return stateMachine;
    }
}
