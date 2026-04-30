package com.sonal.sportsbetting.repository;

import com.sonal.sportsbetting.model.ExposureProjection;
import com.sonal.sportsbetting.model.ExposureProjectionKey;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ExposureProjectionRepository extends JpaRepository<ExposureProjection, ExposureProjectionKey> {

    Optional<ExposureProjection> findByIdScopeAndIdUserId(String scope, String userId);
}
