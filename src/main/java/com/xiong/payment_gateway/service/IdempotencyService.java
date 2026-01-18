package com.xiong.payment_gateway.repository;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import java.util.concurrent.TimeUnit;

@Service
public class IdempotencyService {
    private final RedisTemplate<String, String> redisTemplate;
    private static final String KEY_PREFIX = "idempotency:";
    private static final long TTL_HOURS = 24;

    public IdempotencyService(RedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public boolean isProcessed(String idempotencyKey) {
        return Boolean.TRUE.equals(
            redisTemplate.hasKey(KEY_PREFIX + idempotencyKey)
        );
    }

    public void markAsProcessed(String idempotencyKey, String transactionId) {
        redisTemplate.opsForValue().set(
            KEY_PREFIX + idempotencyKey,
            transactionId,
            TTL_HOURS,
            TimeUnit.HOURS
        );
    }

    public String getTransactionId(String idempotencyKey) {
        return redisTemplate.opsForValue().get(KEY_PREFIX + idempotencyKey);
    }
}