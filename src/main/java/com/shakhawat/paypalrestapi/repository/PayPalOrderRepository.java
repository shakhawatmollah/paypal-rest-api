package com.shakhawat.paypalrestapi.repository;

import com.shakhawat.paypalrestapi.entity.PayPalOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PayPalOrderRepository extends JpaRepository<PayPalOrder, String> {
}

