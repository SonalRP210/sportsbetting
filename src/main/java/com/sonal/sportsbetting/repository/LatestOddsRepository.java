package com.sonal.sportsbetting.repository;

import com.sonal.sportsbetting.model.LatestOdds;
import com.sonal.sportsbetting.model.LatestOddsId;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LatestOddsRepository extends JpaRepository<LatestOdds, LatestOddsId> {
}
