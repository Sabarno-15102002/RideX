package com.ridex.gateway.filter;

import java.util.UUID;

import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;

import reactor.core.publisher.Mono;

@Component
public class CorrelationIdFilter implements GlobalFilter, Ordered {

        public static final String CORRELATION_ID = "X-Correlation-Id";

        @Override
        public Mono<Void> filter(
                        ServerWebExchange exchange,
                        GatewayFilterChain chain) {
                String incomingCorrelationId = exchange.getRequest()
                                .getHeaders()
                                .getFirst(CORRELATION_ID);

                String correlationId = incomingCorrelationId == null || incomingCorrelationId.isBlank()
                                ? UUID.randomUUID().toString()
                                : incomingCorrelationId;

                ServerWebExchange mutatedExchange = exchange.mutate()
                                .request(request -> request
                                                .headers(headers -> headers.set(CORRELATION_ID, correlationId)))
                                .build();

                mutatedExchange.getResponse()
                                .getHeaders()
                                .set(CORRELATION_ID, correlationId);

                return chain.filter(mutatedExchange);
        }

        @Override
        public int getOrder() {
                return Ordered.HIGHEST_PRECEDENCE;
        }
}