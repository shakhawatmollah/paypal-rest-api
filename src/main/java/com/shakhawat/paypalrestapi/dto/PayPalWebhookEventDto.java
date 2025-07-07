package com.shakhawat.paypalrestapi.dto;

import lombok.*;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PayPalWebhookEventDto {
    private Long id;
    private String eventType;
    private String eventData;
    private Instant receivedAt;
}

