package com.sonal.sportsbetting.unit.service;

import com.sonal.sportsbetting.model.OddsUpdate;
import com.sonal.sportsbetting.properties.KafkaBackpressureProperties;
import com.sonal.sportsbetting.service.KafkaOddsFeedPublisher;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import java.util.List;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class KafkaOddsFeedPublisherTest {

    @Mock
    private KafkaTemplate<String, OddsUpdate> kafkaTemplate;

    @Test
    void publishSendsEachEventToConfiguredTopic() {
        KafkaBackpressureProperties properties = new KafkaBackpressureProperties(true, "odds-topic", "group-a");
        KafkaOddsFeedPublisher publisher = new KafkaOddsFeedPublisher(kafkaTemplate, properties);

        OddsUpdate a = new OddsUpdate();
        a.setEventId("e1");
        a.setSelection("home");
        OddsUpdate b = new OddsUpdate();
        b.setEventId("e2");
        b.setSelection("away");

        publisher.publish(List.of(a, b));

        verify(kafkaTemplate).send("odds-topic", "e1", a);
        verify(kafkaTemplate).send("odds-topic", "e2", b);
    }
}
