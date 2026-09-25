package com.ridex.payment.repository;

import com.ridex.payment.entity.PaymentProviderEvent;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentProviderEventRepository extends JpaRepository<PaymentProviderEvent, String> {

    boolean existsByProviderPaymentId(String providerPaymentId);
}