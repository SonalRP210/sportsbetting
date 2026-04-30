package com.sonal.sportsbetting.service.outbox;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class OutboxPoller {
    private static final Logger log = LoggerFactory.getLogger(OutboxPoller.class);

    private final OutboxDispatcher outboxDispatcher;
    private final OutboxRowProcessor outboxRowProcessor;

    public OutboxPoller(OutboxDispatcher outboxDispatcher, OutboxRowProcessor outboxRowProcessor) {
        this.outboxDispatcher = outboxDispatcher;
        this.outboxRowProcessor = outboxRowProcessor;
    }

    @Scheduled(fixedDelayString = "${app.outbox.poll-interval-ms:1000}")
    public void poll() {
        for (var row : outboxDispatcher.fetchUnprocessed()) {
            try {
                outboxRowProcessor.process(row.getId());
            } catch (Exception ex) {
                log.debug("Poller will retry outbox id={}", row.getId(), ex);
            }
        }
    }
}
