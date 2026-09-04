package com.inditex.supplier.infrastructure.persistence.adapter;

import com.inditex.supplier.application.port.out.PotentialSuppliersPage;
import com.inditex.supplier.application.port.out.ScoredSupplier;
import com.inditex.supplier.domain.model.AnnualTurnover;
import com.inditex.supplier.domain.model.CountryCode;
import com.inditex.supplier.domain.model.Duns;
import com.inditex.supplier.domain.model.SupplierRecord;
import com.inditex.supplier.domain.model.SupplierStatus;
import com.inditex.supplier.domain.model.SustainabilityRating;
import com.inditex.supplier.infrastructure.persistence.repository.SupplierRecordJpaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

/**
 * Integration test against a real PostgreSQL Testcontainer — the scoring/bonus/pagination
 * requirement (README §6) must be verified against real SQL, not a mock or an in-memory database
 * with different SQL dialect quirks (H2 doesn't identically support {@code DENSE_RANK()} combined
 * with the double-quoted camelCase column aliases the native query relies on).
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

    private void saveDirect(int duns, String country, long turnover, SupplierStatus status, SustainabilityRating rating) {
        SupplierRecord record = SupplierRecord.reconstitute(new Duns(duns), "Supplier " + duns,
                new CountryCode(country), new AnnualTurnover(turnover), status, rating);
        adapter.save(record);
    }
}
