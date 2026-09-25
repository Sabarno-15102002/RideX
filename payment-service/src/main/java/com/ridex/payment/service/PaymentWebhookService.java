package com.ridex.payment.service;

import com.ridex.payment.dto.PaymentProviderWebhook;

public interface PaymentWebhookService {

    void process(PaymentProviderWebhook webhook);

}
