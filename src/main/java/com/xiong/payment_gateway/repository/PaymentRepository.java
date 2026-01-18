package com.xiong.payment_gateway.repository;

import com.xiong.payment_gateway.models.PaymentTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<PaymentTransaction, String> {
    Optional<PaymentTransaction> findByIdempotencyKey(String idempotencyKey);
}