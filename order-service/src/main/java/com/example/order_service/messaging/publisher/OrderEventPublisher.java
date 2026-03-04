package com.example.order_service.messaging.publisher;

import com.example.order_service.domain.model.Order;

public interface OrderEventPublisher {

    void publish(Order order);
}
