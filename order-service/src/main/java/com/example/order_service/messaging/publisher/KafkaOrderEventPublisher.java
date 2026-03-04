package com.example.order_service.messaging.publisher;

import com.example.order_service.domain.model.Order;
import com.example.order_service.messaging.event.OrderStatusChangedEvent;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class KafkaOrderEventPublisher implements OrderEventPublisher {

    private final KafkaTemplate<String, OrderStatusChangedEvent> kafkaTemplate;
    private final String topic;

    public KafkaOrderEventPublisher(
            KafkaTemplate<String, OrderStatusChangedEvent> kafkaTemplate,
            @Value("${kafka.topics.order-events}") String topic) {
        this.kafkaTemplate = kafkaTemplate;
        this.topic = topic;
    }

    @Override
    public void publish(Order order) {
        kafkaTemplate.send(topic, order.getId().toString(), toEvent(order));
    }

    private OrderStatusChangedEvent toEvent(Order order) {
        return new OrderStatusChangedEvent(
                order.getId(),
                order.getCustomerId(),
                order.getStatus(),
                LocalDateTime.now()
        );
    }
}
