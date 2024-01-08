package guru.sfg.beer.order.service.statemachine;

import guru.sfg.beer.order.service.domain.BeerOrderEventEnum;
import guru.sfg.beer.order.service.domain.BeerOrderStatusEnum;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.statemachine.action.Action;
import org.springframework.statemachine.config.EnableStateMachineFactory;
import org.springframework.statemachine.config.StateMachineConfigurerAdapter;
import org.springframework.statemachine.config.builders.StateMachineStateConfigurer;
import org.springframework.statemachine.config.builders.StateMachineTransitionConfigurer;
import org.springframework.statemachine.config.configurers.ExternalTransitionConfigurer;

import java.util.EnumSet;

import static guru.sfg.beer.order.service.domain.BeerOrderStatusEnum.*;

@EnableStateMachineFactory
@Configuration
@RequiredArgsConstructor
public class BeerOrderStateMachineConfig  extends StateMachineConfigurerAdapter<BeerOrderStatusEnum, BeerOrderEventEnum> {

    private final Action<BeerOrderStatusEnum, BeerOrderEventEnum> validateOrderAction;
    private final Action<BeerOrderStatusEnum, BeerOrderEventEnum> allocateOrderAction;
    private final Action<BeerOrderStatusEnum, BeerOrderEventEnum> validationFailedAction;
    private final Action<BeerOrderStatusEnum, BeerOrderEventEnum> allocationFailureAction;

    @Override
    public void configure(StateMachineStateConfigurer<BeerOrderStatusEnum, BeerOrderEventEnum> states) throws Exception {
        states.withStates()
                .initial(NEW)
                .states(EnumSet.allOf(BeerOrderStatusEnum.class))
                .end(CANCELLED)
                .end(PICKED_UP)
                .end(DELIVERED)
                .end(DELIVERY_ERROR)
                .end(VALIDATION_ERROR)
                .end(ALLOCATION_ERROR);
    }

    @Override
    public void configure(StateMachineTransitionConfigurer<BeerOrderStatusEnum, BeerOrderEventEnum> transitions) throws Exception {
        transitions
                .withExternal().source(NEW).target(VALIDATION_PENDING)
                    .event(BeerOrderEventEnum.VALIDATE_ORDER).action(validateOrderAction)
                .and()
                .withExternal().source(VALIDATION_PENDING).target(VALIDATED)
                    .event(BeerOrderEventEnum.VALIDATION_PASSED)
                .and()
                .withExternal().source(VALIDATION_PENDING).target(VALIDATION_ERROR)
                    .event(BeerOrderEventEnum.VALIDATION_FAILED).action(validationFailedAction)
                .and()
                .withExternal().source(VALIDATION_PENDING).target(CANCELLED)
                    .event(BeerOrderEventEnum.CANCEL_ORDER)
                .and()
                .withExternal().source(VALIDATED).target(ALLOCATION_PENDING)
                    .event(BeerOrderEventEnum.ALLOCATE_ORDER).action(allocateOrderAction)
                .and()
                .withExternal().source(VALIDATED).target(CANCELLED)
                    .event(BeerOrderEventEnum.CANCEL_ORDER)
                .and()
                .withExternal().source(ALLOCATION_PENDING).target(ALLOCATED)
                    .event(BeerOrderEventEnum.ALLOCATION_SUCCESS)
                .and()
                .withExternal().source(ALLOCATION_PENDING).target(ALLOCATION_ERROR)
                    .event(BeerOrderEventEnum.ALLOCATION_FAILED).action(allocationFailureAction)
                .and()
                .withExternal().source(ALLOCATION_PENDING).target(CANCELLED)
                    .event(BeerOrderEventEnum.CANCEL_ORDER)
                .and()
                .withExternal().source(ALLOCATION_PENDING).target(PENDING_INVENTORY)
                    .event(BeerOrderEventEnum.ALLOCATION_NO_INVENTORY)
                .and()
                .withExternal().source(ALLOCATED).target(PICKED_UP)
                    .event(BeerOrderEventEnum.PICK_UP)
                .and()
                .withExternal().source(ALLOCATED).target(CANCELLED)
                    .event(BeerOrderEventEnum.CANCEL_ORDER);
        //TODO  add action
    }
}
