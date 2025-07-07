package com.shakhawat.paypalrestapi.repository;

import com.shakhawat.paypalrestapi.entity.PayPalCapture;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PayPalCaptureRepository extends JpaRepository<PayPalCapture, String> {
}
