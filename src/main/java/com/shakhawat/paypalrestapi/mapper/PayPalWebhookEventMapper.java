package com.shakhawat.paypalrestapi.mapper;

import com.shakhawat.paypalrestapi.dto.PayPalWebhookEventDto;
import com.shakhawat.paypalrestapi.entity.PayPalWebhookEvent;

public class PayPalWebhookEventMapper {

    public static PayPalWebhookEventDto toDto(PayPalWebhookEvent entity) {
        if (entity == null) return null;

        return PayPalWebhookEventDto.builder()
                .id(entity.getId())
                .eventType(entity.getEventType())
                .eventData(entity.getEventData())
                .receivedAt(entity.getReceivedAt())
                .build();
    }

    public static PayPalWebhookEvent toEntity(PayPalWebhookEventDto dto) {
        if (dto == null) return null;

        return PayPalWebhookEvent.builder()
                .id(dto.getId())
                .eventType(dto.getEventType())
                .eventData(dto.getEventData())
                .receivedAt(dto.getReceivedAt())
                .build();
    }
}

