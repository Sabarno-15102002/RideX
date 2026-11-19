package com.ridex.payment.service.provider;

import com.ridex.payment.config.PaymentProperties;
import com.ridex.payment.exception.IllegalOperationException;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PaymentProviderFactoryTest {

    @Test
    void returnsMockProviderForMockConfiguration() {
        PaymentProperties properties = new PaymentProperties();
        properties.setProvider("mock");

        MockPaymentProvider mockProvider = new MockPaymentProvider();
        PaymentProviderFactory factory = new PaymentProviderFactory(properties, mockProvider);

        assertSame(mockProvider, factory.getProvider());
    }

    @Test
    void providerNameIsCaseInsensitive() {
        PaymentProperties properties = new PaymentProperties();
        properties.setProvider("MOCK");

        MockPaymentProvider mockProvider = new MockPaymentProvider();
        PaymentProviderFactory factory = new PaymentProviderFactory(properties, mockProvider);

        assertSame(mockProvider, factory.getProvider());
    }

    @Test
    void unsupportedProviderThrows() {
        PaymentProperties properties = new PaymentProperties();
        properties.setProvider("unknown");

        PaymentProviderFactory factory = new PaymentProviderFactory(properties, new MockPaymentProvider());

        assertThrows(IllegalOperationException.class, factory::getProvider);
    }
}
