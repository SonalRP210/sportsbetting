package com.sonal.sportsbetting.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sonal.sportsbetting.config.AppConstants;
import com.sonal.sportsbetting.model.OddsUpdate;
import com.sonal.sportsbetting.config.CorrelationTestSupport;
import com.sonal.sportsbetting.config.WebMvcFilterTestSupport;
import com.sonal.sportsbetting.service.BettingService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = OddsController.class)
@Import({CorrelationTestSupport.class, WebMvcFilterTestSupport.class})
class OddsControllerMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private BettingService bettingService;

    @Test
    void oddsFeedAccepted() throws Exception {
        OddsUpdate u = new OddsUpdate();
        u.setEventId("evt-1");
        u.setSelection("home");
        u.setOdds(new BigDecimal("1.75"));

        mockMvc.perform(post("/api/v1/odds-feed")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(List.of(u))))
                .andExpect(status().isAccepted())
                .andExpect(content().string(AppConstants.Messages.ODDS_FEED_ACCEPTED));

        verify(bettingService).consumeOddsFeed(anyList());
    }
}
