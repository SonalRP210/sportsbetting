package com.sonal.sportsbetting.service.outbox;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sonal.sportsbetting.domain.event.BetCancelledPayload;
import com.sonal.sportsbetting.domain.event.BetPlacedPayload;
import com.sonal.sportsbetting.domain.event.EventSettledPayload;
import com.sonal.sportsbetting.domain.event.OddsUpdatedPayload;
import com.sonal.sportsbetting.model.DomainEventOutbox;
import com.sonal.sportsbetting.repository.DomainEventOutboxRepository;
import com.sonal.sportsbetting.service.ExposureProjectionUpdater;
import com.sonal.sportsbetting.service.OddsCacheUpdater;
import com.sonal.sportsbetting.service.OddsPublisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Component
public class OutboxDispatcher {
    private static final Logger log = LoggerFactory.getLogger(OutboxDispatcher.class);

    private final DomainEventOutboxRepository outboxRepository;
    private final ObjectMapper objectMapper;
    private final ExposureProjectionUpdater exposureProjectionUpdater;
    private final OddsCacheUpdater oddsCacheUpdater;
    private final OddsPublisher oddsPublisher;
    private final RedisLatestOddsHashWriter redisLatestOddsHashWriter;

    public OutboxDispatcher(
            DomainEventOutboxRepository outboxRepository,
            ObjectMapper objectMapper,
            ExposureProjectionUpdater exposureProjectionUpdater,
            OddsCacheUpdater oddsCacheUpdater,
            OddsPublisher oddsPublisher,
            RedisLatestOddsHashWriter redisLatestOddsHashWriter) {
        this.outboxRepository = outboxRepository;
        this.objectMapper = objectMapper;
        this.exposureProjectionUpdater = exposureProjectionUpdater;
        this.oddsCacheUpdater = oddsCacheUpdater;
        this.oddsPublisher = oddsPublisher;
        this.redisLatestOddsHashWriter = redisLatestOddsHashWriter;
    }

    /**
     * Runs projection and side effects in the caller's transaction (same commit as the outbox row).
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public void processInCurrentTransaction(long id) {
        doProcess(id);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void processById(long id) {
        doProcess(id);
    }

    private void doProcess(long id) {
        DomainEventOutbox row = outboxRepository.findById(id).orElse(null);
        if (row == null) {
            return;
        }
        if (row.getProcessedAt() != null) {
            return;
        }
        try {
            switch (row.getEventType()) {
                case BET_PLACED -> exposureProjectionUpdater.applyBetPlaced(
                        objectMapper.readValue(row.getPayloadJson(), BetPlacedPayload.class));
                case BET_CANCELLED -> exposureProjectionUpdater.applyBetCancelled(
                        objectMapper.readValue(row.getPayloadJson(), BetCancelledPayload.class));
                case EVENT_SETTLED -> exposureProjectionUpdater.applyEventSettled(
                        objectMapper.readValue(row.getPayloadJson(), EventSettledPayload.class));
                case ODDS_UPDATED -> handleOddsUpdated(
                        objectMapper.readValue(row.getPayloadJson(), OddsUpdatedPayload.class));
            }
            row.setProcessedAt(Instant.now());
        } catch (RuntimeException ex) {
            log.error("Outbox processing failed id={} type={}", id, row.getEventType(), ex);
            throw ex;
        } catch (Exception ex) {
            log.error("Outbox processing failed id={} type={}", id, row.getEventType(), ex);
            throw new IllegalStateException(ex);
        }
    }

    private void handleOddsUpdated(OddsUpdatedPayload payload) {
        oddsCacheUpdater.updateFromDispatched(payload);
        oddsPublisher.publish(payload);
        redisLatestOddsHashWriter.writeIfEnabled(payload);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void pollUnprocessed() {
        for (DomainEventOutbox row : outboxRepository.findTop50ByProcessedAtIsNullOrderByIdAsc()) {
            try {
                processById(row.getId());
            } catch (Exception ex) {
                log.debug("Poller will retry outbox id={}", row.getId(), ex);
            }
        }
    }
}
