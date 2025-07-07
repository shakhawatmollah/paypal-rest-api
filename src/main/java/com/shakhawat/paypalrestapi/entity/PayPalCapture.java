package com.shakhawat.paypalrestapi.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class PayPalCapture {

    @Id
    private String captureId;

    private String orderId;

    private Double amount;

    private String currency;

    private String status;

    private String payerEmail;

    private String paymentMethod;

    private String updateTime;
}

