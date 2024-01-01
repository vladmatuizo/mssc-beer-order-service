package com.example.common.model.event;

import com.example.common.model.BeerOrderDto;

public record ValidateOrderRequest(BeerOrderDto beerOrder) {
}
