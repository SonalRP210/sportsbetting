package com.sonal.sportsbetting.support;

import org.junit.jupiter.api.condition.EnabledIf;
import org.springframework.boot.test.context.SpringBootTest;

@EnabledIf("com.sonal.sportsbetting.support.TestEnvironment#dockerAvailable")
@SpringBootTest
public abstract class AbstractPostgresSpringBootTest extends PostgresContainerProvider {
}
