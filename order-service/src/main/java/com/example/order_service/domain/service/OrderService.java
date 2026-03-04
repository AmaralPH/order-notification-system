package com.example.order_service.domain.service;

import com.example.order_service.domain.exception.OrderNotFoundException;
import com.example.order_service.domain.model.Order;
import com.example.order_service.domain.model.OrderStatus;
import com.example.order_service.domain.repository.OrderRepository;
import com.example.order_service.messaging.publisher.OrderEventPublisher;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final OrderEventPublisher orderEventPublisher;

    @Transactional
    public Order createOrder(String customerId, String description, BigDecimal totalAmount) {
        Order order = Order.builder()
                .customerId(customerId)
                .description(description)
                .totalAmount(totalAmount)
                .status(OrderStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .build();

        Order saved = orderRepository.save(order);
        orderEventPublisher.publish(saved);
        return saved;
    }

    public Order findById(UUID id) {
        return orderRepository.findById(id)
                .orElseThrow(() -> new OrderNotFoundException(id));
    }

    @Transactional
    public Order updateStatus(UUID id, OrderStatus newStatus) {
        Order order = findById(id);
        order.transitionTo(newStatus);

        Order saved = orderRepository.save(order);
        orderEventPublisher.publish(saved);
        return saved;
    }
}
