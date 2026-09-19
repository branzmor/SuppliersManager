package com.supplier.management.infrastructure.persistence.adapter;

import com.supplier.management.application.port.out.PotentialSuppliersPage;
import com.supplier.management.application.port.out.ScoredSupplier;
import com.supplier.management.domain.model.AnnualTurnover;
import com.supplier.management.domain.model.CountryCode;
import com.supplier.management.domain.model.Duns;
import com.supplier.management.domain.model.SupplierRecord;
import com.supplier.management.domain.model.SupplierStatus;
import com.supplier.management.domain.model.SustainabilityRating;
import com.supplier.management.infrastructure.persistence.repository.SupplierRecordJpaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

/**
 * Integration test against a real PostgreSQL Testcontainer — the scoring/bonus/pagination
 * requirement (README §6) must be verified against real SQL, not a mock or an in-memory database
 * with different SQL dialect quirks (H2 doesn't identically support {@code DENSE_RANK()} combined
 * with the double-quoted camelCase column aliases the native query relies on).
 *
 * <p>Deliberately does not share its Testcontainer/{@code @SpringBootTest} context with other
 * integration test classes (e.g. {@code ConcurrencyIntegrationTest}) — a shared static container
 * field inherited from a common base class was tried and caused cross-class connection-pool
 * staleness (the second test class to run would get a stopped/restarted container while Spring's
 * context cache still held a HikariCP pool wired to the first container's now-stale port).
 */
@SpringBootTest
@Testcontainers
class SupplierPersistenceAdapterTest {

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

    @BeforeEach
    void cleanDatabase() {
        jpaRepository.deleteAll();
    }

    @Test
    void saveAndFindByDunsRoundTrip() {
        Duns duns = new Duns(123_456_789);
        SupplierRecord candidate = SupplierRecord.apply(duns, "Zippers & Buttons", new CountryCode("ES"),
                new AnnualTurnover(2_000_000L));

        adapter.save(candidate);

        Optional<SupplierRecord> found = adapter.findByDuns(duns);
        assertThat(found).isPresent();
        assertThat(found.get().name()).isEqualTo("Zippers & Buttons");
        assertThat(found.get().country()).isEqualTo(new CountryCode("ES"));
        assertThat(found.get().annualTurnover()).isEqualTo(new AnnualTurnover(2_000_000L));
        assertThat(found.get().status()).isEqualTo(SupplierStatus.CANDIDATE);

        // Upsert semantics: saving again for the same DUNS updates the existing row in place
        // rather than inserting a duplicate (the UNIQUE(duns) constraint is what makes this safe).
        SupplierRecord record = found.get();
        record.accept(SustainabilityRating.A, false);
        adapter.save(record);

        assertThat(jpaRepository.count()).isEqualTo(1);
        SupplierRecord reloaded = adapter.findByDuns(duns).orElseThrow();
        assertThat(reloaded.status()).isEqualTo(SupplierStatus.ACTIVE);
        assertThat(reloaded.sustainabilityRating()).isEqualTo(SustainabilityRating.A);
    }

    @Test
    void findPotentialSuppliersExcludesBannedAndBelowRate() {
        saveDirect(100_000_001, "AR", 2_000_000L, SupplierStatus.ACTIVE, SustainabilityRating.A);
        saveDirect(100_000_002, "AR", 2_000_000L, SupplierStatus.BANNED, SustainabilityRating.A);
        saveDirect(100_000_003, "AR", 100L, SupplierStatus.ACTIVE, SustainabilityRating.A);
        saveDirect(100_000_004, "AR", 2_000_000L, SupplierStatus.CANDIDATE, null);

        PotentialSuppliersPage page = adapter.findPotentialSuppliers(250L, 10, 0);

        assertThat(page.suppliers()).extracting(s -> s.record().duns().value())
                .containsExactly(100_000_001);
        assertThat(page.totalCount()).isEqualTo(1);
    }

    @Test
    void findPotentialSuppliersAppliesSmallSupplierBonusPerReadmeExample() {
        // README worked example: s1:200k, s2:200k, s3:200k, s4:210k, s5:250k -> s1-s4 get the 1.25x bonus.
        saveDirect(200_000_001, "DE", 200_000L, SupplierStatus.ACTIVE, SustainabilityRating.A);
        saveDirect(200_000_002, "DE", 200_000L, SupplierStatus.ACTIVE, SustainabilityRating.A);
        saveDirect(200_000_003, "DE", 200_000L, SupplierStatus.ACTIVE, SustainabilityRating.A);
        saveDirect(200_000_004, "DE", 210_000L, SupplierStatus.ACTIVE, SustainabilityRating.A);
        saveDirect(200_000_005, "DE", 250_000L, SupplierStatus.ACTIVE, SustainabilityRating.A);

        PotentialSuppliersPage page = adapter.findPotentialSuppliers(250L, 10, 0);

        assertThat(page.suppliers())
                .extracting(s -> s.record().duns().value(), ScoredSupplier::score)
                .containsExactlyInAnyOrder(
                        tuple(200_000_001, 25_000.0),
                        tuple(200_000_002, 25_000.0),
                        tuple(200_000_003, 25_000.0),
                        tuple(200_000_004, 26_250.0),
                        tuple(200_000_005, 25_000.0));
    }

    @Test
    void findPotentialSuppliersOrdersByScoreDescendingAndPaginates() {
        saveDirect(300_000_001, "FR", 1_000_000L, SupplierStatus.ACTIVE, SustainabilityRating.E);
        saveDirect(300_000_002, "FR", 5_000_000L, SupplierStatus.ACTIVE, SustainabilityRating.A);
        saveDirect(300_000_003, "FR", 3_000_000L, SupplierStatus.ACTIVE, SustainabilityRating.B);

        PotentialSuppliersPage firstPage = adapter.findPotentialSuppliers(250L, 2, 0);
        assertThat(firstPage.suppliers()).extracting(s -> s.record().duns().value())
                .containsExactly(300_000_002, 300_000_003);
        assertThat(firstPage.totalCount()).isEqualTo(3);

        PotentialSuppliersPage secondPage = adapter.findPotentialSuppliers(250L, 2, 2);
        assertThat(secondPage.suppliers()).extracting(s -> s.record().duns().value())
                .containsExactly(300_000_001);
    }

    @Test
    void findPotentialSuppliersBreaksScoreTiesByDunsAscendingForStablePagination() {
        // Same country, turnover and rating -> identical score for all 4 rows (all also tie for
        // DENSE_RANK=1, so the bonus applies equally to every one of them and doesn't break the
        // tie either). Without an explicit tie-breaker, consecutive LIMIT/OFFSET pages over an
        // ORDER BY score DESC query have no guaranteed order among these rows and could duplicate
        // or skip some when paged.
        int[] dunsValues = {400_000_004, 400_000_002, 400_000_003, 400_000_001};
        for (int duns : dunsValues) {
            saveDirect(duns, "IT", 500_000L, SupplierStatus.ACTIVE, SustainabilityRating.A);
        }

        PotentialSuppliersPage firstPage = adapter.findPotentialSuppliers(250L, 2, 0);
        PotentialSuppliersPage secondPage = adapter.findPotentialSuppliers(250L, 2, 2);

        assertThat(firstPage.suppliers()).extracting(s -> s.record().duns().value())
                .containsExactly(400_000_001, 400_000_002);
        assertThat(secondPage.suppliers()).extracting(s -> s.record().duns().value())
                .containsExactly(400_000_003, 400_000_004);
        assertThat(firstPage.totalCount()).isEqualTo(4);
        assertThat(secondPage.totalCount()).isEqualTo(4);

        // The two pages together must cover every seeded DUNS exactly once - no duplicates, no
        // omissions - regardless of the order rows were inserted in.
        List<Integer> allPagedDuns = new ArrayList<>();
        firstPage.suppliers().forEach(s -> allPagedDuns.add(s.record().duns().value()));
        secondPage.suppliers().forEach(s -> allPagedDuns.add(s.record().duns().value()));
        assertThat(allPagedDuns).containsExactlyInAnyOrder(400_000_001, 400_000_002, 400_000_003, 400_000_004);
        assertThat(Set.copyOf(allPagedDuns)).hasSize(4);
    }

    private void saveDirect(int duns, String country, long turnover, SupplierStatus status, SustainabilityRating rating) {
        SupplierRecord record = SupplierRecord.reconstitute(new Duns(duns), "Supplier " + duns,
                new CountryCode(country), new AnnualTurnover(turnover), status, rating);
        adapter.save(record);
    }
}
