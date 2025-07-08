package com.shakhawat.paypalrestapi.service;

import com.shakhawat.paypalrestapi.dto.PayPalOrderDto;
import com.shakhawat.paypalrestapi.dto.PayPalWebhookEventDto;
import com.shakhawat.paypalrestapi.entity.PayPalCapture;
import com.shakhawat.paypalrestapi.entity.PayPalOrder;
import com.shakhawat.paypalrestapi.entity.PayPalRefund;
import com.shakhawat.paypalrestapi.entity.PayPalWebhookEvent;
import com.shakhawat.paypalrestapi.mapper.PayPalOrderMapper;
import com.shakhawat.paypalrestapi.mapper.PayPalWebhookEventMapper;
import com.shakhawat.paypalrestapi.repository.PayPalCaptureRepository;
import com.shakhawat.paypalrestapi.repository.PayPalOrderRepository;
import com.shakhawat.paypalrestapi.repository.PayPalRefundRepository;
import com.shakhawat.paypalrestapi.repository.PayPalWebhookEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class PayPalDataService {

    private final PayPalOrderRepository orderRepository;
    private final PayPalCaptureRepository captureRepository;
    private final PayPalWebhookEventRepository webhookEventRepository;
    private final PayPalRefundRepository refundRepository;

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

        Optional<PayPalOrder> order = orderRepository.findById(orderId);
        if(order.isPresent()) {
            order.get().setAmount(amount);
            order.get().setCurrency(currency);
            orderRepository.save(order.get());
        }
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

    public void saveRefund(Map<String, Object> resource, String fallbackCaptureId) {
        if (resource == null || resource.isEmpty()) {
            log.warn("Refund resource is null or empty");
            return;
        }

        try {
            String refundId = (String) resource.get("id");
            if (refundId == null) {
                log.warn("No refund ID found in webhook resource");
                return;
            }

            PayPalRefund refund = refundRepository.findById(refundId).orElse(new PayPalRefund());
            refund.setRefundId(refundId);

            // Don't overwrite existing captureId if it was saved from API
            String captureId = (String) resource.get("capture_id");
            if (captureId == null) {
                captureId = (String) resource.get("invoice_id");
            }
            if (captureId == null) {
                captureId = fallbackCaptureId;
            }

            if (refund.getCaptureId() == null && captureId != null) {
                refund.setCaptureId(captureId);
            }

            Map<String, Object> amountObj = (Map<String, Object>) resource.get("amount");
            if (amountObj != null) {
                refund.setAmount(Double.valueOf((String) amountObj.get("value")));
                refund.setCurrency((String) amountObj.get("currency_code"));
            }

            if (resource.get("status") != null) {
                refund.setStatus((String) resource.get("status"));
            }

            if (resource.get("note_to_payer") != null) {
                refund.setReason((String) resource.get("note_to_payer"));
            }

            if (resource.get("create_time") != null) {
                refund.setCreateTime((String) resource.get("create_time"));
            }

            if (resource.get("update_time") != null) {
                refund.setUpdateTime((String) resource.get("update_time"));
            }

            refundRepository.save(refund);
            log.info("Refund [{}] saved/updated", refundId);

        } catch (Exception e) {
            log.error("Failed to save PayPal refund", e);
        }
    }

}
