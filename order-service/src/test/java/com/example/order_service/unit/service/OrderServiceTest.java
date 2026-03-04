package com.example.order_service.unit.service;

import com.example.order_service.domain.exception.InvalidStatusTransitionException;
import com.example.order_service.domain.exception.OrderNotFoundException;
import com.example.order_service.domain.model.Order;
import com.example.order_service.domain.model.OrderStatus;
import com.example.order_service.domain.repository.OrderRepository;
import com.example.order_service.domain.service.OrderService;
import com.example.order_service.messaging.publisher.OrderEventPublisher;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@Tag("unit")
@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private OrderEventPublisher orderEventPublisher;

    @InjectMocks
    private OrderService orderService;

    @Test
    void dado_pedido_valido_quando_criar_deve_salvar_e_retornar_com_id_gerado() {
        Order savedOrder = orderWith(UUID.randomUUID(), OrderStatus.PENDING);
        when(orderRepository.save(any(Order.class))).thenReturn(savedOrder);

        Order result = orderService.createOrder("customer-1", "Notebook", new BigDecimal("2999.90"));

        assertThat(result.getId()).isNotNull();
        assertThat(result.getStatus()).isEqualTo(OrderStatus.PENDING);
        verify(orderRepository).save(any(Order.class));
    }

    @Test
    void dado_pedido_valido_quando_criar_deve_publicar_evento_kafka_com_status_pending() {
        Order savedOrder = orderWith(UUID.randomUUID(), OrderStatus.PENDING);
        when(orderRepository.save(any(Order.class))).thenReturn(savedOrder);

        orderService.createOrder("customer-1", "Notebook", new BigDecimal("2999.90"));

        ArgumentCaptor<Order> captor = ArgumentCaptor.forClass(Order.class);
        verify(orderEventPublisher).publish(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(OrderStatus.PENDING);
    }

    @Test
    void dado_id_existente_quando_buscar_deve_retornar_pedido() {
        UUID id = UUID.randomUUID();
        when(orderRepository.findById(id)).thenReturn(Optional.of(orderWith(id, OrderStatus.PENDING)));

        Order result = orderService.findById(id);

        assertThat(result.getId()).isEqualTo(id);
    }

    @Test
    void dado_id_inexistente_quando_buscar_deve_lancar_OrderNotFoundException() {
        UUID id = UUID.randomUUID();
        when(orderRepository.findById(id)).thenReturn(Optional.empty());

        assertThrows(OrderNotFoundException.class, () -> orderService.findById(id));
    }

    @Test
    void dado_transicao_valida_quando_atualizar_status_deve_persistir_novo_status() {
        UUID id = UUID.randomUUID();
        when(orderRepository.findById(id)).thenReturn(Optional.of(orderWith(id, OrderStatus.PENDING)));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        orderService.updateStatus(id, OrderStatus.CONFIRMED);

        ArgumentCaptor<Order> captor = ArgumentCaptor.forClass(Order.class);
        verify(orderRepository).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(OrderStatus.CONFIRMED);
    }

    @Test
    void dado_transicao_valida_quando_atualizar_status_deve_atualizar_updatedAt() {
        UUID id = UUID.randomUUID();
        LocalDateTime before = LocalDateTime.now();
        when(orderRepository.findById(id)).thenReturn(Optional.of(orderWith(id, OrderStatus.PENDING)));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        orderService.updateStatus(id, OrderStatus.CONFIRMED);

        ArgumentCaptor<Order> captor = ArgumentCaptor.forClass(Order.class);
        verify(orderRepository).save(captor.capture());
        assertThat(captor.getValue().getUpdatedAt()).isNotNull();
        assertThat(captor.getValue().getUpdatedAt()).isAfterOrEqualTo(before);
    }

    @Test
    void dado_transicao_valida_quando_atualizar_status_deve_publicar_evento_kafka() {
        UUID id = UUID.randomUUID();
        when(orderRepository.findById(id)).thenReturn(Optional.of(orderWith(id, OrderStatus.PENDING)));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        orderService.updateStatus(id, OrderStatus.CONFIRMED);

        verify(orderEventPublisher).publish(any(Order.class));
    }

    @Test
    void dado_transicao_invalida_quando_atualizar_status_deve_lancar_InvalidStatusTransitionException() {
        UUID id = UUID.randomUUID();
        when(orderRepository.findById(id)).thenReturn(Optional.of(orderWith(id, OrderStatus.DELIVERED)));

        assertThrows(InvalidStatusTransitionException.class,
                () -> orderService.updateStatus(id, OrderStatus.CONFIRMED));

        verify(orderRepository, never()).save(any());
        verify(orderEventPublisher, never()).publish(any());
    }

    @Test
    void dado_mesmo_status_quando_atualizar_deve_lancar_InvalidStatusTransitionException() {
        UUID id = UUID.randomUUID();
        when(orderRepository.findById(id)).thenReturn(Optional.of(orderWith(id, OrderStatus.PENDING)));

        assertThrows(InvalidStatusTransitionException.class,
                () -> orderService.updateStatus(id, OrderStatus.PENDING));

        verify(orderRepository, never()).save(any());
        verify(orderEventPublisher, never()).publish(any());
    }

    private Order orderWith(UUID id, OrderStatus status) {
        return Order.builder()
                .id(id)
                .customerId("customer-1")
                .description("Notebook")
                .totalAmount(new BigDecimal("2999.90"))
                .status(status)
                .createdAt(LocalDateTime.now())
                .build();
    }
}
