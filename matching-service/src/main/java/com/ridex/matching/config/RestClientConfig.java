package com.ridex.matching.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;
import org.springframework.boot.restclient.autoconfigure.RestClientBuilderConfigurer;

@Configuration
public class RestClientConfig {

    @Bean
    public RestClient.Builder restClientBuilder(
            RestClientBuilderConfigurer configurer) {

        return configurer.configure(RestClient.builder());
    }
}