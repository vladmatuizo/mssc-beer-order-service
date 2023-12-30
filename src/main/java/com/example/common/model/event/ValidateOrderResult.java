package com.example.common.model.event;

import java.util.UUID;

public record ValidateOrderResult(UUID orderId, boolean isValid) {
}
