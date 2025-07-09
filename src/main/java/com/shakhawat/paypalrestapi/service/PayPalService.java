package com.shakhawat.paypalrestapi.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shakhawat.paypalrestapi.entity.PayPalCapture;
import com.shakhawat.paypalrestapi.repository.PayPalCaptureRepository;
import jakarta.annotation.Nullable;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class PayPalService {

    @Value("${paypal.client.id}")
    private String clientId;

    @Value("${paypal.client.secret}")
    private String clientSecret;

    @Value("${paypal.mode:sandbox}")
    private String mode;

    @Getter
    @Value("${paypal.webhook.id}")
    private String webhookId;

    private WebClient webClient;

    private final PayPalDataService  payPalDataService;

    @PostConstruct
    public void init() {
        String baseUrl = "live".equalsIgnoreCase(mode)
                ? "https://api-m.paypal.com"
                : "https://api-m.sandbox.paypal.com";

        this.webClient = WebClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    public Mono<String> getAccessToken() {
        String creds = clientId + ":" + clientSecret;
        String encodedCreds = Base64.getEncoder().encodeToString(creds.getBytes());

        return webClient.post()
                .uri("/v1/oauth2/token")
                .header(HttpHeaders.AUTHORIZATION, "Basic " + encodedCreds)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(BodyInserters.fromFormData("grant_type", "client_credentials"))
                .retrieve()
                .bodyToMono(Map.class)
                .map(response -> (String) response.get("access_token"))
                .doOnError(e -> log.error("Failed to get access token from PayPal", e));
    }

    public Mono<Map<String, Object>> createOrder(String accessToken, Map<String, Object> orderPayload) {
        return webClient.post()
                .uri("/v2/checkout/orders")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                .bodyValue(orderPayload)
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
                .doOnError(e -> log.error("Failed to create PayPal order", e));
    }

    public Mono<Map<String, Object>> captureOrder(String accessToken, String orderId) {
        return webClient.post()
                .uri("/v2/checkout/orders/{orderId}/capture", orderId)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
                .doOnError(e -> log.error("Failed to capture PayPal order", e));
    }

    public Mono<Boolean> verifyWebhookSignature(String accessToken, Map<String, Object> verifyPayload) {
        return webClient.post()
                .uri("/v1/notifications/verify-webhook-signature")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                .bodyValue(verifyPayload)
                .retrieve()
                .bodyToMono(Map.class)
                .map(response -> "SUCCESS".equals(response.get("verification_status")))
                .doOnError(e -> log.error("Webhook verification failed", e));
    }

    public Map<String, Object> buildWebhookVerifyPayload(HttpHeaders headers, Map<String, Object> webhookEvent) {
        Map<String, Object> payload = new HashMap<>();

        payload.put("auth_algo", headers.getFirst("paypal-auth-algo"));
        payload.put("cert_url", headers.getFirst("paypal-cert-url"));
        payload.put("transmission_id", headers.getFirst("paypal-transmission-id"));
        payload.put("transmission_sig", headers.getFirst("paypal-transmission-sig"));
        payload.put("transmission_time", headers.getFirst("paypal-transmission-time"));

        payload.put("webhook_id", webhookId);

        try {
            ObjectMapper mapper = new ObjectMapper();
            String eventBody = mapper.writeValueAsString(webhookEvent);
            payload.put("webhook_event", mapper.readValue(eventBody, Map.class));
        } catch (Exception e) {
            log.error("Failed to build webhook verification payload", e);
        }

        return payload;
    }

    public Mono<Object> refundCapture(String accessToken, String captureId, @Nullable Double amount, @Nullable String currencyCode) {
        Map<String, Object> payload = null;

        if (amount != null && currencyCode != null) {
            payload = Map.of(
                    "amount", Map.of(
                            "value", String.format("%.2f", amount),
                            "currency_code", currencyCode
                    )
            );
        }

        WebClient.RequestBodySpec request = webClient.post()
                .uri("/v2/payments/captures/" + captureId + "/refund")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);

        return (payload != null ? request.bodyValue(payload) : request)
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<>() {})
                .doOnError(e -> log.error("Refund failed for captureId {}", captureId, e));
    }

    public Mono<String> handleCheckoutApprovedWebhook(String orderId, String accessToken) {
        return captureOrder(accessToken, orderId)
                .flatMap(captureResponse -> {
                    try {
                        List<Map<String, Object>> purchaseUnits = (List<Map<String, Object>>) captureResponse.get("purchase_units");

                        if (purchaseUnits != null && !purchaseUnits.isEmpty()) {
                            Map<String, Object> payments = (Map<String, Object>) purchaseUnits.get(0).get("payments");

                            if (payments != null) {
                                List<Map<String, Object>> captures = (List<Map<String, Object>>) payments.get("captures");

                                if (captures != null && !captures.isEmpty()) {
                                    payPalDataService.saveCapture(captures.getFirst());
                                    return Mono.just("Capture saved for order " + orderId);
                                }
                            }
                        }

                        return Mono.just("No captures found in order " + orderId);

                    } catch (Exception e) {
                        log.error("Error parsing capture in webhook capture", e);
                        return Mono.error(new RuntimeException("Failed to save capture"));
                    }
                });
    }

    public Mono<String> handlePaymentCaptureCompletedWebhook(Map<String, Object> captureResource) {
        try {
            if (captureResource == null || captureResource.isEmpty()) {
                return Mono.just("Empty capture resource - nothing to process");
            }

            String captureId = (String) captureResource.get("id");
            String status = (String) captureResource.get("status");

            if (captureId == null) {
                return Mono.just("No capture ID found in resource");
            }

            // Prevent saving duplicate captures (optional, if dataService handles it)
            if (payPalDataService.captureExists(captureId)) {
                log.info("Duplicate capture webhook received for ID: {}", captureId);
                return Mono.just("Duplicate capture ignored");
            }

            // Save capture to database
            payPalDataService.saveCapture(captureResource);

            log.info("Capture [{}] saved with status: {}", captureId, status);
            return Mono.just("Capture processed for ID: " + captureId);

        } catch (Exception e) {
            log.error("Failed to process PAYMENT.CAPTURE.COMPLETED webhook", e);
            return Mono.error(new RuntimeException("Failed to process capture"));
        }
    }

    public Mono<String> getOrderIdFromCapture(String accessToken, String captureId) {
        return webClient.get()
                .uri("/v2/payments/captures/{captureId}", captureId)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                .retrieve()
                .bodyToMono(Map.class)
                .flatMap(captureDetails -> {
                    List<Map<String, Object>> links = (List<Map<String, Object>>) captureDetails.get("links");
                    if (links != null) {
                        for (Map<String, Object> link : links) {
                            if ("up".equals(link.get("rel"))) {
                                String href = (String) link.get("href");
                                String[] parts = href.split("/"); // extract order ID from URL
                                return Mono.just(parts[parts.length - 1]);
                            }
                        }
                    }
                    return Mono.empty();
                });
    }


}
