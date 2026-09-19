package com.supplier.management.application.service;

import com.supplier.management.application.port.in.RegisterCandidateUseCase;
import com.supplier.management.application.port.in.RegisterCandidateUseCase.RegisterCandidateCommand;
import com.supplier.management.domain.exception.CandidateAlreadyExistsException;
import com.supplier.management.domain.model.SupplierRecord;
import com.supplier.management.infrastructure.persistence.repository.SupplierRecordJpaRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Exercises the concurrent-duplicate-DUNS race through the real, {@code @Transactional}
 * {@link RegisterCandidateService} — not {@code SupplierPersistenceAdapter} directly.
 *
 * <p>This distinction matters: {@code SupplierPersistenceAdapter}'s own
 * {@code ConcurrencyIntegrationTest} calls {@code save()} standalone, so each internal repository
 * call (the failed {@code saveAndFlush}, then the "who won" re-read) runs in its own independent
 * mini-transaction — that test could not catch a real bug where the "who won" re-read reused the
 * *same* (by-then poisoned) persistence context as the failed flush, because in
 * {@code RegisterCandidateService} both happen inside one outer {@code @Transactional} method.
 * That bug was only found by firing two genuinely concurrent {@code POST /candidates} at the real
 * running Docker stack (every failed flush turned into an unmapped 500 instead of 409) — this
 * test is the permanent regression test for it, exercising the exact same call shape as the real
 * controller.
 */
@SpringBootTest
@Testcontainers
class RegisterCandidateServiceConcurrencyIntegrationTest {

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    @DynamicPropertySource
    static void datasourceProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @Autowired
    private RegisterCandidateUseCase registerCandidateUseCase;

    @Autowired
    private SupplierRecordJpaRepository jpaRepository;

    private ExecutorService executor;

    @BeforeEach
    void setUp() {
        jpaRepository.deleteAll();
        executor = Executors.newFixedThreadPool(2);
    }

    @AfterEach
    void tearDown() {
        executor.shutdownNow();
    }

    @Test
    void concurrentRegistrationsForTheSameNewDunsResolveToOneWinnerAndOneConflict() throws Exception {
        int duns = 900_000_001;
        CyclicBarrier barrier = new CyclicBarrier(2);

        Callable<SupplierRecord> racer = () -> {
            barrier.await(10, TimeUnit.SECONDS);
            return registerCandidateUseCase.register(
                    new RegisterCandidateCommand(duns, "Racer", "ES", 2_000_000L));
        };

        Future<SupplierRecord> first = executor.submit(racer);
        Future<SupplierRecord> second = executor.submit(racer);

        int successCount = 0;
        int conflictCount = 0;
        for (Future<SupplierRecord> future : List.of(first, second)) {
            try {
                future.get(10, TimeUnit.SECONDS);
                successCount++;
            } catch (ExecutionException e) {
                // The real bug this test guards against surfaced here as an unmapped
                // PersistenceException/DataIntegrityViolationException (HTTP 500 at the
                // controller) instead of this domain exception (HTTP 409).
                assertThat(e.getCause()).isInstanceOf(CandidateAlreadyExistsException.class);
                conflictCount++;
            }
        }

        assertThat(successCount).isEqualTo(1);
        assertThat(conflictCount).isEqualTo(1);
        assertThat(jpaRepository.count()).isEqualTo(1);
    }
}
