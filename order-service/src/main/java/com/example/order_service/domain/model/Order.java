package com.example.order_service.domain.model;

import com.example.order_service.domain.exception.InvalidStatusTransitionException;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "orders")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String customerId;

    @Column(nullable = false)
    private String description;

    @Column(nullable = false)
    private BigDecimal totalAmount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrderStatus status;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    public void transitionTo(OrderStatus newStatus) {
        if (!status.canTransitionTo(newStatus)) {
            throw new InvalidStatusTransitionException(status, newStatus);
        }
        this.status = newStatus;
        this.updatedAt = LocalDateTime.now();
    }
}
