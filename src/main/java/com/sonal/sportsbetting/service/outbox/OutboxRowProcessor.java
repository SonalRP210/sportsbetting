package com.sonal.sportsbetting.service.outbox;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Thin wrapper that gives OutboxPoller a proxied call-site for processById so
 * that REQUIRES_NEW is honoured for each row without self-invocation bypassing
 * Spring AOP.
 */
@Component
public class OutboxRowProcessor {

    private final OutboxDispatcher outboxDispatcher;

    public OutboxRowProcessor(OutboxDispatcher outboxDispatcher) {
        this.outboxDispatcher = outboxDispatcher;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void process(long id) {
        outboxDispatcher.processInCurrentTransaction(id);
    }
}
