package com.shakhawat.paypalrestapi.service;

import com.fasterxml.jackson.databind.ObjectMapper;
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

import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

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

}
