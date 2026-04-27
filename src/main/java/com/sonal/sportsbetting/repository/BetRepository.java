package com.sonal.sportsbetting.repository;

import com.sonal.sportsbetting.model.Bet;
import com.sonal.sportsbetting.model.BetStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface BetRepository extends JpaRepository<Bet, String> {

    List<Bet> findByUserId(String userId);

    List<Bet> findByEventId(String eventId);

    Optional<Bet> findByUserIdAndIdempotencyKey(String userId, String idempotencyKey);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select b from Bet b where b.eventId = :eventId and b.status = :status")
    List<Bet> findByEventIdAndStatusForUpdate(@Param("eventId") String eventId, @Param("status") BetStatus status);

    @Query("select coalesce(sum(b.stake * b.odds), 0) from Bet b where b.status = :status")
    BigDecimal sumExposureByStatus(@Param("status") BetStatus status);
}
