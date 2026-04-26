package com.sonal.sportsbetting.repository;

import com.sonal.sportsbetting.model.EventSettlement;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EventSettlementRepository extends JpaRepository<EventSettlement, String> {
}
