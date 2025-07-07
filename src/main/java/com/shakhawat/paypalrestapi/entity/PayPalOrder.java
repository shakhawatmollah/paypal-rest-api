package com.shakhawat.paypalrestapi.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "paypal_order")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PayPalOrder {

    @Id
    private String orderId;

    private String status;

    private Double amount;

    private String currency;

    private Instant createdAt;

    private Instant updatedAt;
}

