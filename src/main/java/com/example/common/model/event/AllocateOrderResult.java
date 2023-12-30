package com.example.common.model.event;

import com.example.common.model.BeerOrderDto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public final class AllocateOrderResult {
    private BeerOrderDto beerOrder;
    private boolean allocationError = false;
    private boolean pendingInventory = false;
}