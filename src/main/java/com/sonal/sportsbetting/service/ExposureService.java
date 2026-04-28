package com.sonal.sportsbetting.service;

import com.sonal.sportsbetting.dto.response.UserExposureResponse;

import java.math.BigDecimal;

public interface ExposureService {

    BigDecimal getTotalExposure();

    UserExposureResponse getUserExposure(String userId);
}
