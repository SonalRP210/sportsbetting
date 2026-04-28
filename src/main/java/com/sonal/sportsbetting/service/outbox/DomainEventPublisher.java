package com.sonal.sportsbetting.service.outbox;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sonal.sportsbetting.domain.event.DomainEventType;
import com.sonal.sportsbetting.model.DomainEventOutbox;
import com.sonal.sportsbetting.repository.DomainEventOutboxRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Component
public class DomainEventPublisher {
    private static final Logger log = LoggerFactory.getLogger(DomainEventPublisher.class);

    private final DomainEventOutboxRepository outboxRepository;
    private final ObjectMapper objectMapper;
    private final OutboxDispatcher outboxDispatcher;

    public DomainEventPublisher(
            DomainEventOutboxRepository outboxRepository,
            ObjectMapper objectMapper,
            OutboxDispatcher outboxDispatcher) {
        this.outboxRepository = outboxRepository;
        this.objectMapper = objectMapper;
        this.outboxDispatcher = outboxDispatcher;
    }

    @Transactional
    public void publish(DomainEventType type, Object payload) {
        try {
            String json = objectMapper.writeValueAsString(payload);
            DomainEventOutbox row = new DomainEventOutbox(type, json);
            outboxRepository.saveAndFlush(row);
            long id = row.getId();
            if (TransactionSynchronizationManager.isSynchronizationActive()) {
                outboxDispatcher.processInCurrentTransaction(id);
            } else {
                try {
                    outboxDispatcher.processById(id);
                } catch (Exception ex) {
                    log.warn("Outbox dispatch failed for id={}, will rely on poller", id, ex);
                }
            }
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Failed to serialize domain event " + type, ex);
        }
    }
}
