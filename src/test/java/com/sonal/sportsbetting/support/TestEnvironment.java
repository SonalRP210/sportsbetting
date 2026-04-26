package com.sonal.sportsbetting.support;

import org.testcontainers.DockerClientFactory;

/**
 * JUnit {@link org.junit.jupiter.api.condition.EnabledIf} predicates (referenced by string name).
 */
public final class TestEnvironment {

    private TestEnvironment() {
    }

    @SuppressWarnings("unused") // referenced from @EnabledIf("...")
    public static boolean dockerAvailable() {
        try {
            return DockerClientFactory.instance().isDockerAvailable();
        } catch (Throwable e) {
            return false;
        }
    }
}
