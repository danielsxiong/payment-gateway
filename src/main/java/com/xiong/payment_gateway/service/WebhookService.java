package com.xiong.payment_gateway.service;

import com.xiong.payment_gateway.models.PaymentTransaction;
import com.xiong.payment_gateway.models.Refund;
import com.xiong.payment_gateway.models.WebhookEvent;
import com.xiong.payment_gateway.models.WebhookStatus;
import com.xiong.payment_gateway.repository.WebhookRepository;

import lombok.extern.slf4j.Slf4j;
import tools.jackson.databind.ObjectMapper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import java.util.HashMap;
import java.util.Map;

@Service
@Slf4j
public class WebhookService {
    private final WebhookRepository webhookRepository;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public WebhookService(
        WebhookRepository webhookRepository,
        RestTemplate restTemplate,
        ObjectMapper objectMapper
    ) {
        this.webhookRepository = webhookRepository;
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
    }

    @Async
    public void sendWebhook(PaymentTransaction transaction, String webhookUrl) {
        try {
            Map<String, Object> payload = new HashMap<>();
            payload.put("event_type", "payment.completed");
            payload.put("transaction_id", transaction.getId());
            payload.put("status", transaction.getStatus());
            payload.put("amount", transaction.getAmount());
            payload.put("currency", transaction.getCurrency());

            WebhookEvent event = new WebhookEvent();
            event.setTransactionId(transaction.getId());
            event.setEventType("payment.completed");
            event.setPayload(objectMapper.writeValueAsString(payload));
            event.setWebhookUrl(webhookUrl);
            event.setStatus(WebhookStatus.PENDING);
            
            event = webhookRepository.save(event);
            
            // Attempt delivery
            deliverWebhook(event);
        } catch (Exception e) {
            log.error("Error creating webhook", e);
        }
    }

    @Async
    public void sendRefundWebhook(Refund refund, PaymentTransaction transaction) {
        // Similar implementation for refund webhooks
        log.info("Sending refund webhook for refund: {}", refund.getId());
    }

    private void deliverWebhook(WebhookEvent event) {
        try {
            restTemplate.postForEntity(
                event.getWebhookUrl(),
                event.getPayload(),
                String.class
            );
            
            event.setStatus(WebhookStatus.DELIVERED);
            webhookRepository.save(event);
            log.info("Webhook delivered successfully: {}", event.getId());
        } catch (Exception e) {
            event.setAttempts(event.getAttempts() + 1);
            event.setStatus(WebhookStatus.FAILED);
            webhookRepository.save(event);
            log.error("Webhook delivery failed: {}", event.getId(), e);
        }
    }
}
