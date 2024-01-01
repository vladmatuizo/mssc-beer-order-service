package com.example.common.model.event;

import com.example.common.model.BeerOrderDto;

public record AllocateOrderRequest(BeerOrderDto beerOrder) {
}
