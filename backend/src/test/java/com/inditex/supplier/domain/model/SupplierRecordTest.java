package com.inditex.supplier.domain.model;

import com.inditex.supplier.domain.exception.CandidateNotAcceptableException;
import com.inditex.supplier.domain.exception.CandidateNotRefusableException;
import com.inditex.supplier.domain.exception.SupplierNotBannableException;
import com.inditex.supplier.domain.exception.SupplierNotPromotableException;
import com.inditex.supplier.domain.exception.SupplierNotRestrictableException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Core business-logic unit tests for {@link SupplierRecord} — plain domain tests, no Spring
 * context. Covers the state machine transitions and guards described in the class javadoc and in
 * {@code SOLUTION.md}.
 */
class SupplierRecordTest {

    private static final Duns DUNS = new Duns(123_456_789);
    private static final String NAME = "Zippers & Buttons";
    private static final CountryCode APPROVED_COUNTRY = new CountryCode("ES");
    private static final AnnualTurnover SUFFICIENT_TURNOVER = new AnnualTurnover(2_000_000L);
    private static final AnnualTurnover INSUFFICIENT_TURNOVER = new AnnualTurnover(500_000L);

    private static SupplierRecord candidate() {
        return SupplierRecord.apply(DUNS, NAME, APPROVED_COUNTRY, SUFFICIENT_TURNOVER);
    }

    private static SupplierRecord recordIn(SupplierStatus status) {
        return recordIn(status, status == SupplierStatus.CANDIDATE || status == SupplierStatus.REFUSED ? null : SustainabilityRating.B);
    }

    private static SupplierRecord recordIn(SupplierStatus status, SustainabilityRating rating) {
        return SupplierRecord.reconstitute(DUNS, NAME, APPROVED_COUNTRY, SUFFICIENT_TURNOVER, status, rating);
    }

    @Test
    void applyCreatesRecordInCandidateStatus() {
        SupplierRecord record = candidate();

        assertThat(record.duns()).isEqualTo(DUNS);
        assertThat(record.name()).isEqualTo(NAME);
        assertThat(record.country()).isEqualTo(APPROVED_COUNTRY);
        assertThat(record.annualTurnover()).isEqualTo(SUFFICIENT_TURNOVER);
        assertThat(record.status()).isEqualTo(SupplierStatus.CANDIDATE);
        assertThat(record.sustainabilityRating()).isNull();
    }

    @Test
    void acceptWithGoodRatingBecomesActive() {
        SupplierRecord recordA = candidate();
        recordA.accept(SustainabilityRating.A, false);
        assertThat(recordA.status()).isEqualTo(SupplierStatus.ACTIVE);
        assertThat(recordA.sustainabilityRating()).isEqualTo(SustainabilityRating.A);

        SupplierRecord recordB = candidate();
        recordB.accept(SustainabilityRating.B, false);
        assertThat(recordB.status()).isEqualTo(SupplierStatus.ACTIVE);
        assertThat(recordB.sustainabilityRating()).isEqualTo(SustainabilityRating.B);
    }

    @Test
    void acceptWithPoorRatingBecomesOnProbation() {
        for (SustainabilityRating rating : new SustainabilityRating[] {SustainabilityRating.C, SustainabilityRating.D, SustainabilityRating.E}) {
            SupplierRecord record = candidate();
            record.accept(rating, false);
            assertThat(record.status()).isEqualTo(SupplierStatus.ON_PROBATION);
            assertThat(record.sustainabilityRating()).isEqualTo(rating);
        }
    }

    @Test
    void acceptFailsWhenNotCandidate() {
        SupplierRecord active = recordIn(SupplierStatus.ACTIVE);

        assertThatThrownBy(() -> active.accept(SustainabilityRating.A, false))
                .isInstanceOf(CandidateNotAcceptableException.class);
        assertThat(active.status()).isEqualTo(SupplierStatus.ACTIVE);
    }

    @Test
    void acceptFailsWhenCountryBanned() {
        SupplierRecord record = candidate();

        assertThatThrownBy(() -> record.accept(SustainabilityRating.A, true))
                .isInstanceOf(CandidateNotAcceptableException.class);
        assertThat(record.status()).isEqualTo(SupplierStatus.CANDIDATE);
    }

    @Test
    void acceptFailsWhenTurnoverBelowMinimum() {
        SupplierRecord record = SupplierRecord.apply(DUNS, NAME, APPROVED_COUNTRY, INSUFFICIENT_TURNOVER);

        assertThatThrownBy(() -> record.accept(SustainabilityRating.A, false))
                .isInstanceOf(CandidateNotAcceptableException.class);
        assertThat(record.status()).isEqualTo(SupplierStatus.CANDIDATE);
    }

    @Test
    void refuseFailsWhenNotCandidate() {
        SupplierRecord record = candidate();

        record.refuse();
        assertThat(record.status()).isEqualTo(SupplierStatus.REFUSED);

        // REFUSED is terminal (no-reapply project decision) — refusing again must fail the same
        // way as refusing any other non-CANDIDATE status.
        assertThatThrownBy(record::refuse).isInstanceOf(CandidateNotRefusableException.class);
    }

    @Test
    void banOnlyValidFromOnProbationNotFromActive() {
        SupplierRecord onProbation = recordIn(SupplierStatus.ON_PROBATION);
        onProbation.ban();
        assertThat(onProbation.status()).isEqualTo(SupplierStatus.BANNED);

        SupplierRecord active = recordIn(SupplierStatus.ACTIVE);
        assertThatThrownBy(active::ban).isInstanceOf(SupplierNotBannableException.class);
        assertThat(active.status()).isEqualTo(SupplierStatus.ACTIVE);
    }

    @Test
    void restrictOnlyValidFromActive() {
        SupplierRecord active = recordIn(SupplierStatus.ACTIVE);
        active.restrict();
        assertThat(active.status()).isEqualTo(SupplierStatus.ON_PROBATION);

        SupplierRecord candidateRecord = candidate();
        assertThatThrownBy(candidateRecord::restrict).isInstanceOf(SupplierNotRestrictableException.class);
        assertThat(candidateRecord.status()).isEqualTo(SupplierStatus.CANDIDATE);
    }

    @Test
    void promoteOnlyValidFromOnProbation() {
        SupplierRecord onProbation = recordIn(SupplierStatus.ON_PROBATION);
        onProbation.promote();
        assertThat(onProbation.status()).isEqualTo(SupplierStatus.ACTIVE);

        SupplierRecord active = recordIn(SupplierStatus.ACTIVE);
        assertThatThrownBy(active::promote).isInstanceOf(SupplierNotPromotableException.class);
        assertThat(active.status()).isEqualTo(SupplierStatus.ACTIVE);
    }

    @Test
    void isVisibleAsCandidateOnlyForCandidateAndRefused() {
        assertThat(recordIn(SupplierStatus.CANDIDATE).isVisibleAsCandidate()).isTrue();
        assertThat(recordIn(SupplierStatus.REFUSED).isVisibleAsCandidate()).isTrue();
        assertThat(recordIn(SupplierStatus.ACTIVE).isVisibleAsCandidate()).isFalse();
        assertThat(recordIn(SupplierStatus.ON_PROBATION).isVisibleAsCandidate()).isFalse();
        assertThat(recordIn(SupplierStatus.BANNED).isVisibleAsCandidate()).isFalse();
    }

    @Test
    void isVisibleAsSupplierOnlyForActiveOnProbationAndBanned() {
        assertThat(recordIn(SupplierStatus.ACTIVE).isVisibleAsSupplier()).isTrue();
        assertThat(recordIn(SupplierStatus.ON_PROBATION).isVisibleAsSupplier()).isTrue();
        assertThat(recordIn(SupplierStatus.BANNED).isVisibleAsSupplier()).isTrue();
        assertThat(recordIn(SupplierStatus.CANDIDATE).isVisibleAsSupplier()).isFalse();
        assertThat(recordIn(SupplierStatus.REFUSED).isVisibleAsSupplier()).isFalse();
    }

    @Test
    void refusedStatusIsTerminalNoReapply() {
        SupplierRecord refused = recordIn(SupplierStatus.REFUSED);

        // No reapply() method exists on the aggregate at all (confirmed no-reapply project
        // decision — see SupplierStatus javadoc). Every mutating operation must reject a REFUSED
        // record, proving there is no outgoing transition.
        assertThatThrownBy(() -> refused.accept(SustainabilityRating.A, false))
                .isInstanceOf(CandidateNotAcceptableException.class);
        assertThatThrownBy(refused::refuse).isInstanceOf(CandidateNotRefusableException.class);
        assertThatThrownBy(refused::ban).isInstanceOf(SupplierNotBannableException.class);
        assertThatThrownBy(refused::restrict).isInstanceOf(SupplierNotRestrictableException.class);
        assertThatThrownBy(refused::promote).isInstanceOf(SupplierNotPromotableException.class);
        assertThat(refused.status()).isEqualTo(SupplierStatus.REFUSED);
    }
}
