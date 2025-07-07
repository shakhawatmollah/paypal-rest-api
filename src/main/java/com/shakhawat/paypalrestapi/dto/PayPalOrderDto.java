package com.shakhawat.paypalrestapi.dto;

import lombok.*;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PayPalOrderDto {
    private String orderId;
    private String status;
    private Double amount;
    private String currency;
    private Instant createdAt;
    private Instant updatedAt;
}

