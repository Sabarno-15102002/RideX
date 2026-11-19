package com.ridex.location;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.hibernate.autoconfigure.HibernateJpaAutoConfiguration;
import org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration;

@SpringBootTest(properties = {
                "spring.kafka.listener.auto-startup=false",
                "eureka.client.enabled=false",
                "spring.cloud.discovery.enabled=false",
                "management.health.redis.enabled=false"
})
@EnableAutoConfiguration(exclude = {
                DataSourceAutoConfiguration.class,
                HibernateJpaAutoConfiguration.class
})
class LocationServiceApplicationTests {

        @Test
        void contextLoads() {
        }
}