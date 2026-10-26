package br.com.comandas;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.*;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import java.time.*;
import java.util.concurrent.atomic.AtomicReference;

public abstract class IntegrationSupport {
    private static PostgreSQLContainer<?> postgres;

    @DynamicPropertySource
    static void database(DynamicPropertyRegistry registry) {
        String externa = System.getenv("TEST_DB_URL");
        if (externa != null && !externa.isBlank()) {
            registry.add("spring.datasource.url", () -> externa);
            registry.add("spring.datasource.username", () -> System.getenv().getOrDefault("TEST_DB_USERNAME", "postgres"));
            registry.add("spring.datasource.password", () -> System.getenv().getOrDefault("TEST_DB_PASSWORD", ""));
        } else {
            if (postgres == null) {
                postgres = new PostgreSQLContainer<>("postgres:17.6-alpine");
                postgres.start();
            }
            registry.add("spring.datasource.url", postgres::getJdbcUrl);
            registry.add("spring.datasource.username", postgres::getUsername);
            registry.add("spring.datasource.password", postgres::getPassword);
        }
    }

    @TestConfiguration
    public static class TimeConfiguration {
        @Bean @Primary
        MutableClock testClock() { return new MutableClock(); }
    }

    public static class MutableClock extends Clock {
        private final AtomicReference<Instant> instante = new AtomicReference<>(Instant.parse("2026-10-26T15:00:00Z"));
        public void set(String valor) { instante.set(Instant.parse(valor)); }
        @Override public ZoneId getZone() { return ZoneOffset.UTC; }
        @Override public Clock withZone(ZoneId zone) { return Clock.fixed(instant(), zone); }
        @Override public Instant instant() { return instante.get(); }
    }
}

