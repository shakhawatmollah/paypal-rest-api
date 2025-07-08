package com.shakhawat.paypalrestapi.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "paypal_refunds")
public class PayPalRefund {

    @Id
    private String refundId;

    private String captureId;

    private Double amount;

    private String currency;

    private String status;

    private String reason; // optional

    private String createTime;

    private String updateTime;
}
