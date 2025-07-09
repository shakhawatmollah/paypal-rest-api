# Spring Boot PayPal REST API Integration

This project provides a modern, non-blocking integration with **PayPal REST APIs** using **Spring Boot 3.5**, **WebClient**, and **Reactive WebFlux**. It includes full support for order creation, capture, refunds, and webhook handling.

---

## 🚀 Features

- ✅ Create PayPal Orders
- ✅ Redirect customers to PayPal for payment
- ✅ Capture authorized payments
- ✅ Issue full or partial refunds
- ✅ Handle PayPal Webhook events
- ✅ Store refund data to PostgreSQL
- ✅ Designed with reactive, non-blocking architecture

---

## 📦 Tech Stack

- Java 21
- Spring Boot 3.4 (WebFlux)
- WebClient (non-blocking HTTP)
- PostgreSQL + Spring Data JPA
- Lombok (optional)
- PayPal v2 Orders API

---

## 📂 Project Structure

```
src/
├── controller/            # REST API controllers
├── service/               # PayPal API, webhook, data services
├── model/                 # Entities like PayPalRefund
├── repository/            # Spring Data repositories
└── config/                # WebClient config
```

---

## 📜 API Endpoints (for Postman)

| Method | Endpoint | Description |
|--------|----------|-------------|
| `POST` | `/api/paypal/order` | Create order and get redirect URL |
| `GET`  | `/api/paypal/success?token=` | Handle return from PayPal after payment |
| `GET`  | `/api/paypal/cancel` | Handle cancel return URL |
| `POST` | `/api/paypal/refund/{captureId}` | Refund payment (full/partial) |
| `POST` | `/api/paypal/webhook` | Receive and verify PayPal webhook |

---

## 🔐 Webhook Setup

1. Use `ngrok` or a tunnel to expose your `/webhook` endpoint.
2. Register webhook at [PayPal Developer Dashboard](https://developer.paypal.com/).
3. Required headers for verification:
   - `paypal-transmission-id`
   - `paypal-cert-url`
   - `paypal-auth-algo`
   - `paypal-transmission-time`
   - `paypal-transmission-sig`

---

## 🛠️ Configuration

Add the following in `application.yml` or `application.properties`:

```
paypal:
  client-id: YOUR_CLIENT_ID
  client-secret: YOUR_SECRET
  base-url: https://api-m.sandbox.paypal.com
  webhook-id: YOUR_WEBHOOK_ID
```
---
## ✅ Sample Final User Flow
1. Client: /create-order → PayPal approve URL
2. User approves → redirected to /return?orderId=XYZ
3. Server: /capture-order/{orderId} → confirms + shows success
4. Webhook (optional): handles fallback capture

---

## 🙏 Credits

Developed by Shakhawat Mollah