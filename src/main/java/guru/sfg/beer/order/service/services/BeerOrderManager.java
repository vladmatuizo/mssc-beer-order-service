package guru.sfg.beer.order.service.services;

import guru.sfg.beer.order.service.domain.BeerOrder;

import java.util.UUID;

public interface BeerOrderManager {

    BeerOrder create(BeerOrder beerOrder);

    void processValidationResult(UUID orderId, boolean isValid);
}
