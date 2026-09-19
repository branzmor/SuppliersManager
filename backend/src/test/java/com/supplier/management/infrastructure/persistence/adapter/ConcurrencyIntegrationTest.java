package com.supplier.management.infrastructure.persistence.adapter;

import com.supplier.management.domain.exception.CandidateAlreadyExistsException;
import com.supplier.management.domain.model.AnnualTurnover;
import com.supplier.management.domain.model.CountryCode;
import com.supplier.management.domain.model.Duns;
import com.supplier.management.domain.model.SupplierRecord;
import com.supplier.management.domain.model.SupplierStatus;
import com.supplier.management.domain.model.SustainabilityRating;
import com.supplier.management.infrastructure.persistence.repository.SupplierRecordJpaRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.DefaultTransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;
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
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Integration tests proving the two concurrency guarantees added on top of
 * {@link SupplierPersistenceAdapter} actually hold against real PostgreSQL, not just in theory:
 * <ul>
 *   <li>{@link #concurrentUpdatesToTheSameRowDoNotSilentlyOverwriteEachOther()} — optimistic
 *       locking (the {@code @Version} column) prevents a lost update between two overlapping
 *       transactions that both read the same row before either commits.</li>
 *   <li>{@link #concurrentInsertsForTheSameDunsAreSerializedByTheUniqueConstraint()} — the
 *       {@code findByDuns}-then-{@code save} race described in the class's own javadoc: two
 *       genuinely concurrent {@code save()} calls for a brand-new, identical DUNS must resolve to
 *       exactly one winner and one {@link CandidateAlreadyExistsException}, never a duplicate row
 *       and never an unmapped 500.</li>
 * </ul>
 *
 * <p>Uses its own dedicated Testcontainer/{@code @SpringBootTest} context rather than sharing one
 * with {@code SupplierPersistenceAdapterTest} — see that class's javadoc for why.
 */
@SpringBootTest
@Testcontainers
class ConcurrencyIntegrationTest {

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    @DynamicPropertySource
    static void datasourceProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @Autowired
    private SupplierPersistenceAdapter adapter;

    @Autowired
    private SupplierRecordJpaRepository jpaRepository;

    @Autowired
    private PlatformTransactionManager transactionManager;

    private TransactionTemplate transactionTemplate;
    private ExecutorService executor;

    @BeforeEach
    void setUp() {
        jpaRepository.deleteAll();
        transactionTemplate = new TransactionTemplate(transactionManager);
        // PROPAGATION_REQUIRES_NEW: request A must run in a genuinely independent transaction,
        // not merely nest inside whatever transaction is already bound to the current thread
        // (transaction B, held open for the duration of the test below) - REQUIRES_NEW suspends
        // that ambient transaction/session for the duration of the block and resumes it
        // afterwards, which is what lets a single test thread simulate two isolated, overlapping
        // "requests" without needing real OS threads.
        transactionTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        executor = Executors.newFixedThreadPool(2);
    }

    @AfterEach
    void tearDown() {
        executor.shutdownNow();
    }

    @Test
    void concurrentUpdatesToTheSameRowDoNotSilentlyOverwriteEachOther() {
        Duns duns = new Duns(600_000_001);
        adapter.save(SupplierRecord.apply(duns, "Original Name", new CountryCode("ES"), new AnnualTurnover(2_000_000L)));

        // Manually demarcate transaction B (request B) so it can be held open across request A's
        // full read-mutate-commit cycle - reproducing "two overlapping HTTP requests, B's read
        // happens before A's commit" without relying on timing-sensitive real thread scheduling.
        // Everything from here on is wrapped in try/finally: an abandoned, never-committed-or-
        // rolled-back transaction would otherwise leak a pooled connection into every later test.
        TransactionStatus transactionB = transactionManager.getTransaction(new DefaultTransactionDefinition());
        try {
            SupplierRecord recordSeenByRequestB = adapter.findByDuns(duns).orElseThrow();

            // Request A: independent, complete transaction (PROPAGATION_REQUIRES_NEW suspends B
            // for the duration of this block) - reads the same starting row, refuses it, and
            // commits. Version goes 0 -> 1.
            transactionTemplate.executeWithoutResult(status -> {
                SupplierRecord recordSeenByRequestA = adapter.findByDuns(duns).orElseThrow();
                recordSeenByRequestA.refuse();
                adapter.save(recordSeenByRequestA);
            });

            // Confirm A's commit really landed - read via another fresh REQUIRES_NEW transaction,
            // not through B's own session (which would still show the pre-A snapshot it read
            // earlier, since a session doesn't see external commits mid-transaction).
            SupplierRecord afterA = transactionTemplate.execute(status -> adapter.findByDuns(duns).orElseThrow());
            assertThat(afterA.status()).isEqualTo(SupplierStatus.REFUSED);

            // Request B resumes: it still only knows about the version-0 snapshot it read before A
            // committed. Applying its own (individually valid) mutation and trying to persist it
            // must fail loudly instead of silently clobbering A's already-committed REFUSED status.
            recordSeenByRequestB.accept(SustainabilityRating.A, false);
            assertThatThrownBy(() -> adapter.save(recordSeenByRequestB))
                    .isInstanceOf(ObjectOptimisticLockingFailureException.class);
        } finally {
            transactionManager.rollback(transactionB);
        }

        // The lost update was prevented: A's committed change is still there, untouched by B.
        SupplierRecord finalState = adapter.findByDuns(duns).orElseThrow();
        assertThat(finalState.status()).isEqualTo(SupplierStatus.REFUSED);
    }

    @Test
    void concurrentInsertsForTheSameDunsAreSerializedByTheUniqueConstraint() throws Exception {
        Duns duns = new Duns(600_000_002);
        CyclicBarrier barrier = new CyclicBarrier(2);

        Callable<SupplierRecord> racer = () -> {
            barrier.await(10, TimeUnit.SECONDS);
            SupplierRecord candidate = SupplierRecord.apply(duns, "Racer", new CountryCode("ES"), new AnnualTurnover(2_000_000L));
            return adapter.save(candidate);
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
                assertThat(e.getCause()).isInstanceOf(CandidateAlreadyExistsException.class);
                conflictCount++;
            }
        }

        assertThat(successCount).isEqualTo(1);
        assertThat(conflictCount).isEqualTo(1);
        assertThat(jpaRepository.findByDuns(duns.value())).isPresent();
        assertThat(jpaRepository.count()).isEqualTo(1);
    }
}
