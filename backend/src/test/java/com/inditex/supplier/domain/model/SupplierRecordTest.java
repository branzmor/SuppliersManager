package com.inditex.supplier.domain.model;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

/**
 * TODO coverage — the core of the business logic, no Spring context needed (plain domain unit
 * tests). At minimum:
 * <ul>
 *   <li>{@code apply()} builds a record in {@code CANDIDATE} status with the given fields.</li>
 *   <li>{@code accept(A|B, countryBanned=false)} with turnover &gt;= 1M → {@code ACTIVE}.</li>
 *   <li>{@code accept(C|D|E, countryBanned=false)} with turnover &gt;= 1M → {@code ON_PROBATION}.</li>
 *   <li>{@code accept} on non-CANDIDATE status → {@code CandidateNotAcceptableException}.</li>
 *   <li>{@code accept} with {@code countryBanned=true} → {@code CandidateNotAcceptableException}
 *       (regardless of rating/turnover).</li>
 *   <li>{@code accept} with {@code annualTurnover < 1_000_000} → {@code CandidateNotAcceptableException}.</li>
 *   <li>{@code refuse()} from {@code CANDIDATE} → {@code REFUSED}; from any other status →
 *       {@code CandidateNotRefusableException}.</li>
 *   <li>{@code ban()} from {@code ON_PROBATION} → {@code BANNED}; from {@code ACTIVE} (and any
 *       other status) → {@code SupplierNotBannableException} (confirmed decision: ACTIVE is
 *       NOT bannable directly).</li>
 *   <li>{@code restrict()} from {@code ACTIVE} → {@code ON_PROBATION}; otherwise
 *       {@code SupplierNotRestrictableException} (extension, no controller yet).</li>
 *   <li>{@code promote()} from {@code ON_PROBATION} → {@code ACTIVE}; otherwise
 *       {@code SupplierNotPromotableException} (extension, no controller yet).</li>
 *   <li>{@code isVisibleAsCandidate()} true only for {@code CANDIDATE}/{@code REFUSED}.</li>
 *   <li>{@code isVisibleAsSupplier()} true only for {@code ACTIVE}/{@code ON_PROBATION}/{@code BANNED}.</li>
 *   <li>No {@code reapply()} method exists / {@code REFUSED} has no outgoing transition — assert
 *       this is a terminal status (see {@code SupplierStatus#isTerminal}), covering the confirmed
 *       no-reapply project decision.</li>
 * </ul>
 */
class SupplierRecordTest {

    @Test
    @Disabled("TODO: implement - see class javadoc")
    void applyCreatesRecordInCandidateStatus() {
    }

    @Test
    @Disabled("TODO: implement - see class javadoc")
    void acceptWithGoodRatingBecomesActive() {
    }

    @Test
    @Disabled("TODO: implement - see class javadoc")
    void acceptWithPoorRatingBecomesOnProbation() {
    }

    @Test
    @Disabled("TODO: implement - see class javadoc")
    void acceptFailsWhenNotCandidate() {
    }

    @Test
    @Disabled("TODO: implement - see class javadoc")
    void acceptFailsWhenCountryBanned() {
    }

    @Test
    @Disabled("TODO: implement - see class javadoc")
    void acceptFailsWhenTurnoverBelowMinimum() {
    }

    @Test
    @Disabled("TODO: implement - see class javadoc")
    void refuseFailsWhenNotCandidate() {
    }

    @Test
    @Disabled("TODO: implement - see class javadoc")
    void banOnlyValidFromOnProbationNotFromActive() {
    }

    @Test
    @Disabled("TODO: implement - see class javadoc")
    void restrictOnlyValidFromActive() {
    }

    @Test
    @Disabled("TODO: implement - see class javadoc")
    void promoteOnlyValidFromOnProbation() {
    }

    @Test
    @Disabled("TODO: implement - see class javadoc")
    void refusedStatusIsTerminalNoReapply() {
    }
}
