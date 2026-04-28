package com.sonal.sportsbetting.service.outbox;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class OutboxPoller {

    private final OutboxDispatcher outboxDispatcher;

    public OutboxPoller(OutboxDispatcher outboxDispatcher) {
        this.outboxDispatcher = outboxDispatcher;
    }

    @Scheduled(fixedDelayString = "${app.outbox.poll-interval-ms:1000}")
    public void poll() {
        outboxDispatcher.pollUnprocessed();
    }
}
