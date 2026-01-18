package com.xiong.payment_gateway.repository;

import com.xiong.payment_gateway.models.WebhookEvent;
import com.xiong.payment_gateway.models.WebhookStatus;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface WebhookRepository extends JpaRepository<WebhookEvent, String> {
    List<WebhookEvent> findByStatus(WebhookStatus status);
}
