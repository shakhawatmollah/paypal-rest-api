package com.shakhawat.paypalrestapi.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shakhawat.paypalrestapi.service.PayPalDataService;
import com.shakhawat.paypalrestapi.service.PayPalService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/paypal")
@RequiredArgsConstructor
public class PayPalController {

    private final PayPalService payPalService;
    private final PayPalDataService dataService;

    @PostMapping("/create-order")
    public Mono<ResponseEntity<Map<String, String>>> createOrder(@RequestBody Map<String, Object> orderPayload) {
        return payPalService.getAccessToken()
                .flatMap(token -> payPalService.createOrder(token, orderPayload)
                        .flatMap(orderResponse -> {
                            String orderId = (String) orderResponse.get("id");
                            String status = (String) orderResponse.get("status");

                            // Extract approval URL
                            List<Map<String, String>> links = (List<Map<String, String>>) orderResponse.get("links");
                            String approvalUrl = links.stream()
                                    .filter(link -> "approve".equals(link.get("rel")))
                                    .findFirst()
                                    .map(link -> link.get("href"))
                                    .orElse(null);

                            // (Optional) save only basic info here, capture full detail after user returns
                            dataService.saveOrder(orderId, status, null, null); // or just skip

                            return Mono.just(ResponseEntity.ok(Map.of("approvalUrl", approvalUrl)));
                        })
                )
                .onErrorResume(e -> {
                    log.error("Failed to create order", e);
                    return Mono.just(ResponseEntity.internalServerError().body(Map.of("error", e.getMessage())));
                });
    }

    @PostMapping("/capture-order/{orderId}")
    public Mono<ResponseEntity<?>> captureOrder(@PathVariable String orderId) {
        return payPalService.getAccessToken()
                .flatMap(token -> payPalService.captureOrder(token, orderId)
                        .flatMap(captureResponse -> {
                            try {
                                // Navigate response structure
                                List<Map<String, Object>> purchaseUnits = (List<Map<String, Object>>) captureResponse.get("purchase_units");
                                Map<String, Object> purchaseUnit = purchaseUnits.get(0);

                                Map<String, Object> payments = (Map<String, Object>) purchaseUnit.get("payments");
                                List<Map<String, Object>> captures = (List<Map<String, Object>>) payments.get("captures");
                                Map<String, Object> capture = captures.get(0);

                                String captureId = (String) capture.get("id");
                                String status = (String) capture.get("status");
                                String updateTime = (String) capture.get("update_time");

                                Map<String, Object> amount = (Map<String, Object>) capture.get("amount");
                                Double value = Double.valueOf((String) amount.get("value"));
                                String currency = (String) amount.get("currency_code");

                                Map<String, Object> payer = (Map<String, Object>) captureResponse.get("payer");
                                String payerEmail = payer != null ? (String) payer.get("email_address") : null;

                                String paymentMethod = "PayPal";

                                // Save captured data (can make this async if needed)
                                dataService.saveCapturedPayment(orderId, captureId, value, currency, status, payerEmail, paymentMethod, updateTime);

                                // Return response
                                return Mono.just((ResponseEntity<?>) ResponseEntity.ok(Map.of(
                                        "status", status,
                                        "value", value,
                                        "currency", currency,
                                        "captureId", captureId
                                )));

                            } catch (Exception e) {
                                log.error("Failed to parse capture response", e);
                                return Mono.just(ResponseEntity.internalServerError().body(Map.of("error", "Unexpected capture structure")));
                            }
                        }))
                .onErrorResume(e -> {
                    log.error("Failed to capture order", e);
                    return Mono.just(ResponseEntity.internalServerError().body(Map.of("error", e.getMessage())));
                });
    }

    @GetMapping("/paypal/success")
    public ResponseEntity<String> success(@RequestParam("token") String orderId) {
        return ResponseEntity.ok("Payment approved! Order ID: " + orderId);
    }

    @GetMapping("/paypal/cancel")
    public ResponseEntity<String> cancel() {
        return ResponseEntity.ok("Payment cancelled by user.");
    }

    @PostMapping("/refund/{captureId}")
    public Mono<ResponseEntity<Object>> refundCapture(
            @PathVariable String captureId,
            @RequestBody(required = false) Map<String, Object> body
    ) {
        Double amount;
        String currency;

        if (body != null && body.containsKey("amount")) {
            Map<String, Object> amountObj = (Map<String, Object>) body.get("amount");
            amount = Double.valueOf((String) amountObj.get("value"));
            currency = (String) amountObj.get("currency_code");
        } else {
            currency = "";
            amount = null;
        }

        return payPalService.getAccessToken()
                .flatMap(token -> payPalService.refundCapture(token, captureId, amount, currency)
                        .flatMap(refundResponse -> {
                            if (refundResponse instanceof Map<?, ?>) {
                                dataService.saveRefund((Map<String, Object>) refundResponse, captureId);
                            }
                            return Mono.just(ResponseEntity.ok(refundResponse));
                        }))
                .onErrorResume(e -> {
                    log.error("Refund error", e);
                    return Mono.just(ResponseEntity.status(500).body(Map.of("error", e.getMessage())));
                });

    }

}

