package com.shakhawat.paypalrestapi.service;

import com.shakhawat.paypalrestapi.dto.PayPalOrderDto;
import com.shakhawat.paypalrestapi.dto.PayPalWebhookEventDto;
import com.shakhawat.paypalrestapi.entity.PayPalCapture;
import com.shakhawat.paypalrestapi.entity.PayPalOrder;
import com.shakhawat.paypalrestapi.entity.PayPalWebhookEvent;
import com.shakhawat.paypalrestapi.mapper.PayPalOrderMapper;
import com.shakhawat.paypalrestapi.mapper.PayPalWebhookEventMapper;
import com.shakhawat.paypalrestapi.repository.PayPalCaptureRepository;
import com.shakhawat.paypalrestapi.repository.PayPalOrderRepository;
import com.shakhawat.paypalrestapi.repository.PayPalWebhookEventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class PayPalDataService {

    private final PayPalOrderRepository orderRepository;
    private final PayPalCaptureRepository captureRepository;
    private final PayPalWebhookEventRepository webhookEventRepository;

    public PayPalOrderDto saveOrder(String orderId, String status, Double amount, String currency) {
        PayPalOrder entity = PayPalOrder.builder()
                .orderId(orderId)
                .status(status)
                .amount(amount)
                .currency(currency)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
        entity = orderRepository.save(entity);
        return PayPalOrderMapper.toDto(entity);
    }

    public void saveCapturedPayment(
            String orderId,
            String captureId,
            Double amount,
            String currency,
            String status,
            String payerEmail,
            String paymentMethod,
            String updateTime
    ) {
        PayPalCapture capture = PayPalCapture.builder()
                .captureId(captureId)
                .orderId(orderId)
                .amount(amount)
                .currency(currency)
                .status(status)
                .payerEmail(payerEmail)
                .paymentMethod(paymentMethod)
                .updateTime(updateTime)
                .build();

        captureRepository.save(capture);
    }

    public PayPalWebhookEventDto saveWebhookEvent(String eventType, String eventData) {
        PayPalWebhookEvent entity = PayPalWebhookEvent.builder()
                .eventType(eventType)
                .eventData(eventData)
                .receivedAt(Instant.now())
                .build();
        entity = webhookEventRepository.save(entity);
        return PayPalWebhookEventMapper.toDto(entity);
    }
}
