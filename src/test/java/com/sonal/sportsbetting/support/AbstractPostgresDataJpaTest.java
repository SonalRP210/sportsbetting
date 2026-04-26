package com.sonal.sportsbetting.support;

import org.junit.jupiter.api.condition.EnabledIf;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

@EnabledIf("com.sonal.sportsbetting.support.TestEnvironment#dockerAvailable")
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
public abstract class AbstractPostgresDataJpaTest extends PostgresContainerProvider {
}
