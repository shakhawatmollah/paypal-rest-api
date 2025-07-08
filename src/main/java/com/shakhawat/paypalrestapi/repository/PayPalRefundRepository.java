package com.shakhawat.paypalrestapi.repository;

import com.shakhawat.paypalrestapi.entity.PayPalRefund;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PayPalRefundRepository extends JpaRepository<PayPalRefund, String> {
}

