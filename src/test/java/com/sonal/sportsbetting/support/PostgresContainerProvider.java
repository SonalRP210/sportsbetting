package com.sonal.sportsbetting.support;

import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.DockerClientFactory;

/**
 * Single PostgreSQL container for the whole test JVM (survives across {@code @SpringBootTest} classes).
 * <p>
 * JUnit's {@code @Container} lifecycle stops the container after each test <em>class</em>, while Spring
 * often <strong>reuses</strong> the same {@code ApplicationContext} across classes that share configuration.
 * That leaves Hikari pointing at a dead host/port. A manually started singleton avoids that mismatch.
 * <p>
 * When Docker is unavailable, subclasses should be skipped via {@code @EnabledIf} on the abstract bases
 * so this class is not loaded for those tests.
 */
public abstract class PostgresContainerProvider {

    private static final PostgreSQLContainer<?> POSTGRES = startIfDockerUp();

    private static PostgreSQLContainer<?> startIfDockerUp() {
        try {
            if (!DockerClientFactory.instance().isDockerAvailable()) {
                return null;
            }
            PostgreSQLContainer<?> container = new PostgreSQLContainer<>("postgres:16-alpine")
                    .withDatabaseName("sportsbetting_test")
                    .withUsername("test")
                    .withPassword("test");
            container.start();
            Runtime.getRuntime().addShutdownHook(new Thread(container::stop));
            return container;
        } catch (Throwable e) {
            return null;
        }
    }

    @DynamicPropertySource
    static void registerPostgresProperties(DynamicPropertyRegistry registry) {
        if (POSTGRES == null || !POSTGRES.isRunning()) {
            throw new IllegalStateException(
                    "PostgreSQL test container is not running. Enable Docker or exclude DB-backed tests.");
        }
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
        registry.add("spring.jpa.properties.hibernate.dialect", () -> "org.hibernate.dialect.PostgreSQLDialect");
    }
}
