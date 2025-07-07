package com.shakhawat.paypalrestapi.mapper;

import com.shakhawat.paypalrestapi.dto.PayPalOrderDto;
import com.shakhawat.paypalrestapi.entity.PayPalOrder;

public class PayPalOrderMapper {

    public static PayPalOrderDto toDto(PayPalOrder entity) {
        if (entity == null) return null;

        return PayPalOrderDto.builder()
                .orderId(entity.getOrderId())
                .status(entity.getStatus())
                .amount(entity.getAmount())
                .currency(entity.getCurrency())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    public static PayPalOrder toEntity(PayPalOrderDto dto) {
        if (dto == null) return null;

        return PayPalOrder.builder()
                .orderId(dto.getOrderId())
                .status(dto.getStatus())
                .amount(dto.getAmount())
                .currency(dto.getCurrency())
                .createdAt(dto.getCreatedAt())
                .updatedAt(dto.getUpdatedAt())
                .build();
    }
}

