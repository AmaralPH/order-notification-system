package com.example.order_service.messaging.event;

import com.example.order_service.domain.model.OrderStatus;

import java.time.LocalDateTime;
import java.util.UUID;

public record OrderStatusChangedEvent(
        UUID orderId,
        String customerId,
        OrderStatus status,
        LocalDateTime occurredAt
) {}
