package com.example.order_service.unit.messaging;

import com.example.order_service.domain.model.Order;
import com.example.order_service.domain.model.OrderStatus;
import com.example.order_service.messaging.event.OrderStatusChangedEvent;
import com.example.order_service.messaging.publisher.KafkaOrderEventPublisher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;

@Tag("unit")
@ExtendWith(MockitoExtension.class)
class KafkaOrderEventPublisherTest {

    @Mock
    private KafkaTemplate<String, OrderStatusChangedEvent> kafkaTemplate;

    private KafkaOrderEventPublisher publisher;

    @BeforeEach
    void setUp() {
        publisher = new KafkaOrderEventPublisher(kafkaTemplate, "order.events");
    }

    @Test
    void dado_order_quando_publicar_deve_enviar_evento_para_topico_correto() {
        publisher.publish(orderWith(OrderStatus.PENDING));

        ArgumentCaptor<String> topicCaptor = ArgumentCaptor.forClass(String.class);
        verify(kafkaTemplate).send(topicCaptor.capture(), anyString(), any(OrderStatusChangedEvent.class));
        assertThat(topicCaptor.getValue()).isEqualTo("order.events");
    }

    @Test
    void dado_order_quando_publicar_deve_usar_orderId_como_chave_da_mensagem() {
        Order order = orderWith(OrderStatus.PENDING);

        publisher.publish(order);

        ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
        verify(kafkaTemplate).send(anyString(), keyCaptor.capture(), any(OrderStatusChangedEvent.class));
        assertThat(keyCaptor.getValue()).isEqualTo(order.getId().toString());
    }

    @Test
    void dado_order_quando_publicar_deve_mapear_id_customerId_e_status_corretamente() {
        Order order = orderWith(OrderStatus.CONFIRMED);

        publisher.publish(order);

        ArgumentCaptor<OrderStatusChangedEvent> eventCaptor = ArgumentCaptor.forClass(OrderStatusChangedEvent.class);
        verify(kafkaTemplate).send(anyString(), anyString(), eventCaptor.capture());

        OrderStatusChangedEvent event = eventCaptor.getValue();
        assertThat(event.orderId()).isEqualTo(order.getId());
        assertThat(event.customerId()).isEqualTo(order.getCustomerId());
        assertThat(event.status()).isEqualTo(OrderStatus.CONFIRMED);
        assertThat(event.occurredAt()).isNotNull();
    }

    private Order orderWith(OrderStatus status) {
        return Order.builder()
                .id(UUID.randomUUID())
                .customerId("customer-1")
                .description("Notebook")
                .totalAmount(new BigDecimal("2999.90"))
                .status(status)
                .createdAt(LocalDateTime.now())
                .build();
    }
}
