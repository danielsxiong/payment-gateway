package com.xiong.payment_gateway.models;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Table(name = "webhook_events")
@Data
public class WebhookEvent {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(nullable = false)
    private String transactionId;

    @Column(nullable = false)
    private String eventType;

    @Column(columnDefinition = "TEXT")
    private String payload;

    @Column(nullable = false)
    private String webhookUrl;

    @Enumerated(EnumType.STRING)
    private WebhookStatus status;

    private Integer attempts = 0;

    private LocalDateTime nextRetry;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}