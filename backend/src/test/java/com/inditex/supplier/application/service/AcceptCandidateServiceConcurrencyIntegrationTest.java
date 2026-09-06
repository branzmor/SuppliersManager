package com.inditex.supplier.application.service;

import com.inditex.supplier.application.port.in.AcceptCandidateUseCase;
import com.inditex.supplier.application.port.out.CountryCheckPort;
import com.inditex.supplier.application.port.out.SupplierRepositoryPort;
import com.inditex.supplier.domain.model.AnnualTurnover;
import com.inditex.supplier.domain.model.CountryCode;
import com.inditex.supplier.domain.model.Duns;
import com.inditex.supplier.domain.model.SupplierRecord;
import com.inditex.supplier.domain.model.SupplierStatus;
import com.inditex.supplier.domain.model.SustainabilityRating;
import com.inditex.supplier.infrastructure.persistence.repository.SupplierRecordJpaRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

/**
 * Proves that {@link AcceptCandidateService#accept}, the real {@code @Transactional} use case
 * behind {@code POST /candidates/{duns}/accept}, never persists a half-applied state transition
 * when an optimistic-locking conflict occurs — this is the "conflicto de optimistic locking
 * durante una transición" scenario called out as priority 1 for rollback verification.
 *
 * <p>{@code ConcurrencyIntegrationTest#concurrentUpdatesToTheSameRowDoNotSilentlyOverwriteEachOther}
 * already proves the underlying mechanism (the {@code @Version} column genuinely prevents a lost
 * update against real PostgreSQL) at the {@code SupplierPersistenceAdapter} level, with the test
 * itself manually demarcating two overlapping transactions via {@code TransactionTemplate}. This
 * test is deliberately not a duplicate of that one: it never touches a transaction API directly —
 * it fires two real concurrent threads at the actual {@link AcceptCandidateUseCase} port (the same
 * shape {@code AcceptCandidateController} would use), the way
 * {@code RegisterCandidateServiceConcurrencyIntegrationTest} does for the insert race — and its
 * assertion is stronger than "the old value survived": the two racing calls request two
 * *different* outcomes (rating {@code A} -> {@code ACTIVE} vs. rating {@code D} ->
 * {@code ON_PROBATION}), so a bug that let the loser's write partially land would surface as an
 * impossible combination (e.g. status {@code ACTIVE} with rating {@code D}), not just a stale
 * value.
 *
 * <p>{@link CountryCheckPort} is mocked — not the {@link AcceptCandidateUseCase} under test, just
 * the one collaborator that would otherwise require a real country-service HTTP call. Its mocked
 * {@code isBanned} answer doubles as the test's synchronization point: {@code accept()} calls it
 * strictly between the read ({@code findByDuns}) and the write ({@code record.accept(...)} +
 * {@code save}), so blocking both racing threads there on a shared {@link CyclicBarrier} guarantees
 * — deterministically, not "usually" — that both threads have already read the same
 * {@code version = 0} row before either one is allowed to proceed to its write. Without this, a
 * barrier placed only around the outer {@code accept()} call (synchronizing thread *start*, not the
 * read) would leave a small window where one thread could race all the way through read-check-write
 * and commit before the other thread even performs its read — occasionally producing two successful
 * writes and zero conflicts instead of the race this test exists to force.
 */
@SpringBootTest
@Testcontainers
class AcceptCandidateServiceConcurrencyIntegrationTest {

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    @DynamicPropertySource
    static void datasourceProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @Autowired
    private AcceptCandidateUseCase acceptCandidateUseCase;

    @Autowired
    private SupplierRepositoryPort supplierRepositoryPort;

    @Autowired
    private SupplierRecordJpaRepository jpaRepository;

    @MockBean
    private CountryCheckPort countryCheckPort;

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
    void concurrentAcceptCallsForTheSameCandidateNeverPersistAMixOfBothOutcomes() throws Exception {
        Duns duns = new Duns(950_000_001);
        supplierRepositoryPort.save(
                SupplierRecord.apply(duns, "Racer", new CountryCode("ES"), new AnnualTurnover(2_000_000L)));

        // The barrier sits inside the mocked collaborator, between the read and the write of
        // AcceptCandidateService#accept - see this class's javadoc for why that placement (rather
        // than a barrier around the whole accept() call) is what makes the race deterministic.
        CyclicBarrier readBarrier = new CyclicBarrier(2);
        given(countryCheckPort.isBanned(any())).willAnswer(invocation -> {
            readBarrier.await(10, TimeUnit.SECONDS);
            return false;
        });

        Callable<Void> acceptWithRatingA = () -> {
            acceptCandidateUseCase.accept(duns.value(), SustainabilityRating.A);
            return null;
        };
        Callable<Void> acceptWithRatingD = () -> {
            acceptCandidateUseCase.accept(duns.value(), SustainabilityRating.D);
            return null;
        };

        Future<Void> first = executor.submit(acceptWithRatingA);
        Future<Void> second = executor.submit(acceptWithRatingD);

        int successCount = 0;
        int conflictCount = 0;
        for (Future<Void> future : List.of(first, second)) {
            try {
                future.get(10, TimeUnit.SECONDS);
                successCount++;
            } catch (ExecutionException e) {
                assertThat(e.getCause()).isInstanceOf(ObjectOptimisticLockingFailureException.class);
                conflictCount++;
            }
        }

        // Exactly one of the two full transitions committed; the loser's write never landed, not
        // even partially - the transaction rolled back as a whole, as @Transactional guarantees.
        assertThat(successCount).as("exactly one accept() call must win the race").isEqualTo(1);
        assertThat(conflictCount).as("exactly one accept() call must lose the race").isEqualTo(1);

        assertThat(jpaRepository.count()).isEqualTo(1);
        SupplierRecord finalState = supplierRepositoryPort.findByDuns(duns).orElseThrow();

        boolean matchesRatingAOutcome =
                finalState.status() == SupplierStatus.ACTIVE && finalState.sustainabilityRating() == SustainabilityRating.A;
        boolean matchesRatingDOutcome =
                finalState.status() == SupplierStatus.ON_PROBATION && finalState.sustainabilityRating() == SustainabilityRating.D;

        // The critical assertion: the persisted row must be EXACTLY one of the two complete,
        // internally-consistent outcomes - never an impossible mix such as ACTIVE+D or
        // ON_PROBATION+A, which is what a partially-applied (non-atomic) transition would produce.
        assertThat(matchesRatingAOutcome ^ matchesRatingDOutcome)
                .as("final state must be exactly one complete transition, never a mix: status=%s, rating=%s",
                        finalState.status(), finalState.sustainabilityRating())
                .isTrue();
    }
}
