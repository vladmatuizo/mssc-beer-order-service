package guru.sfg.beer.order.service.services;

import com.example.common.model.BeerOrderDto;
import guru.sfg.beer.order.service.domain.BeerOrder;

import java.util.UUID;

public interface BeerOrderManager {

    BeerOrder create(BeerOrder beerOrder);

    void processValidationResult(UUID orderId, boolean isValid);
    void processSuccessfulBeerOrderAllocation(BeerOrderDto beerOrder);
    void processBeerOrderAllocationPendingInventory(BeerOrderDto beerOrder);
    void processBeerOrderAllocationFailure(BeerOrderDto beerOrder);
    void processBeerOrderPickUp(UUID beerId);
    void processBeerOrderCancel(UUID beerId);
}
