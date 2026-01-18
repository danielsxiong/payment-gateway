package com.xiong.payment_gateway.repository;

import com.xiong.payment_gateway.models.Refund;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface RefundRepository extends JpaRepository<Refund, String> {
    List<Refund> findByTransactionId(String transactionId);
}
