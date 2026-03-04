package com.example.order_service.domain.exception;

import com.example.order_service.domain.model.OrderStatus;

public class InvalidStatusTransitionException extends RuntimeException {

    public InvalidStatusTransitionException(OrderStatus from, OrderStatus to) {
        super("Invalid status transition from " + from + " to " + to);
    }
}
