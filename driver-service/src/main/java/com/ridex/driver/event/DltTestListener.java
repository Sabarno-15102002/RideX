package com.ridex.driver.event;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(
        name = "ridex.kafka.dlt-test.enabled",
        havingValue = "true"
)
public class DltTestListener {

    @KafkaListener(
            topics = "ridex.dlt.test",
            groupId = "ridex-dlt-test-group",
            containerFactory = "dltTestKafkaListenerContainerFactory"
    )
    public void listen(String message) {

        System.out.println("DLT test received: " + message);

        throw new RuntimeException(
                "Intentional failure for DLT verification"
        );
    }
}