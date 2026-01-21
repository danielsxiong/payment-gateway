package com.xiong.payment_gateway.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.*;

/**
 * Test controller for webhook development and testing.
 * This endpoint receives webhook events sent by the payment gateway
 * and logs them for inspection.
 * 
 * Usage: Set webhookUrl to "http://localhost:8080/api/v1/webhooks/test" in PaymentRequest
 */
@RestController
@RequestMapping("/api/v1/webhooks")
@Slf4j
public class WebhookTestController {
    
    // In-memory storage for recent webhook events (for testing only)
    private static final List<Map<String, Object>> webhookHistory = Collections.synchronizedList(new ArrayList<>());
    private static final int MAX_HISTORY = 50;

    /**
     * Receive webhook events from the payment gateway
     * 
     * @param payload the webhook payload as JSON string
     * @return confirmation of webhook receipt
     */
    @PostMapping("/test")
    public ResponseEntity<?> receiveWebhook(@RequestBody String payload) {
        if (payload == null || payload.isEmpty()) {
            log.warn("Empty webhook payload received");
            return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(Map.of(
                    "status", "error",
                    "message", "Webhook payload cannot be empty"
                ));
        }

        log.info("Webhook received: {}", payload);
        
        // Store in history
        Map<String, Object> event = new HashMap<>();
        event.put("receivedAt", new Date());
        event.put("payload", payload);
        
        webhookHistory.add(0, event);
        
        // Keep only recent events
        if (webhookHistory.size() > MAX_HISTORY) {
            webhookHistory.remove(webhookHistory.size() - 1);
        }
        
        return ResponseEntity
            .status(HttpStatus.OK)
            .body(Map.of(
                "status", "received",
                "message", "Webhook received successfully"
            ));
    }

    /**
     * Retrieve webhook history
     * 
     * @return list of received webhook events
     */
    @GetMapping("/test/history")
    public ResponseEntity<?> getWebhookHistory() {
        return ResponseEntity.ok(Map.of(
            "totalReceived", webhookHistory.size(),
            "events", webhookHistory
        ));
    }

    /**
     * Clear webhook history
     * 
     * @return confirmation of history cleared
     */
    @DeleteMapping("/test/history")
    public ResponseEntity<?> clearWebhookHistory() {
        webhookHistory.clear();
        return ResponseEntity
            .status(HttpStatus.NO_CONTENT)
            .build();
    }
}
