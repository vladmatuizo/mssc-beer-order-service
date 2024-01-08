package com.example.common.model.event;

import com.example.common.model.BeerOrderDto;

public record DeallocateOrderRequest(BeerOrderDto beerOrder) {
}
