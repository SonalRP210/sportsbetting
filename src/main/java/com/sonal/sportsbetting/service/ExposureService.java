package com.sonal.sportsbetting.service;

import com.sonal.sportsbetting.dto.UserExposureResponse;

import java.math.BigDecimal;

public interface ExposureService {
    void increaseExposure(BigDecimal amount);

    void decreaseExposure(BigDecimal amount);

    BigDecimal getTotalExposure();

    UserExposureResponse getUserExposure(String userId);
}
