package com.ridex.gateway.config;

import io.micrometer.context.ContextRegistry;
import jakarta.annotation.PostConstruct;
import org.slf4j.MDC;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MdcContextConfig {

    private static final String CORRELATION_ID = "correlationId";

    @PostConstruct
    public void registerMdcAccessor() {
        ContextRegistry.getInstance()
                .registerThreadLocalAccessor(
                        CORRELATION_ID,
                        () -> MDC.get(CORRELATION_ID),
                        value -> MDC.put(CORRELATION_ID, value),
                        () -> MDC.remove(CORRELATION_ID)
                );
    }
}