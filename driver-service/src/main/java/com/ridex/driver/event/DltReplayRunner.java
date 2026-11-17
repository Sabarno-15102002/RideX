package com.ridex.driver.event;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(
        name = "ridex.kafka.dlt-replay.enabled",
        havingValue = "true"
)
public class DltReplayRunner implements ApplicationRunner {

    private final DltReplayOperations dltReplayOperations;

    public DltReplayRunner(DltReplayOperations dltReplayOperations) {
        this.dltReplayOperations = dltReplayOperations;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {

        String testId = args.getOptionValues("test-id") == null
                ? null
                : args.getOptionValues("test-id").get(0);

        if (testId == null || testId.isBlank()) {
            throw new IllegalArgumentException(
                    "Pass --test-id=<testId> to select the DLT record"
            );
        }

        dltReplayOperations.replay(testId);
    }
}
