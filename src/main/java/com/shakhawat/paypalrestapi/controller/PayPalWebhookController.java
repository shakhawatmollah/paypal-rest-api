package com.shakhawat.paypalrestapi.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shakhawat.paypalrestapi.service.PayPalDataService;
import com.shakhawat.paypalrestapi.service.PayPalService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/paypal")
@RequiredArgsConstructor
public class PayPalWebhookController {

    private final PayPalService payPalService;
    private final PayPalDataService dataService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @PostMapping("/webhook")
    public Mono<ResponseEntity<String>> handleWebhook(@RequestHeader HttpHeaders headers, @RequestBody Map<String, Object> webhookEvent) {

        return payPalService.getAccessToken().flatMap(accessToken -> {
            Map<String, Object> verifyPayload = payPalService.buildWebhookVerifyPayload(headers, webhookEvent);
            return payPalService.verifyWebhookSignature(accessToken, verifyPayload).flatMap(isValid -> {
                if (Boolean.TRUE.equals(isValid)) {
                    try {
                        String eventType = (String) webhookEvent.get("event_type");
                        String eventJson = new ObjectMapper().writeValueAsString(webhookEvent);
                        Map<String, Object> resource = (Map<String, Object>) webhookEvent.get("resource");

                        dataService.saveWebhookEvent(eventType, eventJson);

                        if ("CHECKOUT.ORDER.APPROVED".equalsIgnoreCase(eventType)) {
                            String orderId = (String) resource.get("id");
                            return payPalService.handleCheckoutApprovedWebhook(orderId, accessToken)
                                    .map(ResponseEntity::ok)
                                    .onErrorResume(e -> {
                                        log.error("Auto-capture failed in webhook", e);
                                        return Mono.just(ResponseEntity.status(500).body("Auto-capture failed"));
                                    });
                        }

                        if ("PAYMENT.CAPTURE.COMPLETED".equalsIgnoreCase(eventType)) {
                            String captureId = (String) resource.get("id");

                            return payPalService.handlePaymentCaptureCompletedWebhook(resource)
                                    .map(ResponseEntity::ok)
                                    .onErrorResume(e -> {
                                        log.error("Capture handling failed", e);
                                        return Mono.just(ResponseEntity.status(500).body("Capture handling failed"));
                                    });
                        }

                        // Handle refund webhook
                        if ("PAYMENT.CAPTURE.REFUNDED".equals(eventType)) {
                            String captureId = (String) resource.get("capture_id");
                            if (captureId == null) {
                                captureId = (String) resource.get("invoice_id");
                            }
                            dataService.saveRefund(resource, captureId);
                            log.info("Refund captured via webhook: {}", resource.get("id"));
                        }

                        log.info("Webhook event processed: {}", eventType);
                        return Mono.just(ResponseEntity.ok("Webhook processed"));
                    } catch (Exception e) {
                        log.error("Failed to process webhook event", e);
                        return Mono.just(ResponseEntity.status(500).body("Internal Server Error"));
                    }
                } else {
                    log.warn("Invalid webhook signature");
                    return Mono.just(ResponseEntity.badRequest().body("Invalid webhook signature"));
                }
            });
        }).onErrorResume(e -> {
            log.error("Webhook processing error", e);
            return Mono.just(ResponseEntity.status(500).body("Internal Server Error"));
        });
    }


}
