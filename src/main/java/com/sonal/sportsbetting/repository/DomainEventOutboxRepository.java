package com.sonal.sportsbetting.repository;

import com.sonal.sportsbetting.model.DomainEventOutbox;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DomainEventOutboxRepository extends JpaRepository<DomainEventOutbox, Long> {

    List<DomainEventOutbox> findTop50ByProcessedAtIsNullOrderByIdAsc();
}
