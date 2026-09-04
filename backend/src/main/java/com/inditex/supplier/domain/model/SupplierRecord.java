package com.inditex.supplier.domain.model;

import com.inditex.supplier.domain.exception.CandidateAlreadyExistsException;
import com.inditex.supplier.domain.exception.CandidateNotAcceptableException;
import com.inditex.supplier.domain.exception.CandidateNotRefusableException;
import com.inditex.supplier.domain.exception.SupplierBannedException;
import com.inditex.supplier.domain.exception.SupplierNotBannableException;

/**
 * Aggregate root of the bounded context. Identity = {@link Duns}.
 *
 * <p>A single aggregate replaces the more obvious {@code Candidate}/{@code Supplier} entity
 * split. Because both "candidate" and "supplier" are just different views over the same DUNS
 * lifecycle, modelling them as one aggregate with one {@code UNIQUE(duns)} constraint makes the
 * three integrity rules in README §"Integrity Rules" automatic instead of something enforced by
 * cross-table checks:
 * <ul>
 *   <li>Only one active candidacy can exist for a given DUNS.</li>
 *   <li>Only one supplier can exist for a given DUNS.</li>
 *   <li>An active candidate and a supplier can never coexist for the same DUNS.</li>
 * </ul>
 *
 * <h2>State machine (5 internal states, see {@link SupplierStatus})</h2>
 * <pre>
 *   CANDIDATE --accept(rating A|B)--&gt;   ACTIVE
 *   CANDIDATE --accept(rating C|D|E)--&gt; ON_PROBATION
 *   CANDIDATE --refuse()--&gt;             REFUSED           (terminal — see below)
 *   ON_PROBATION --ban()--&gt;             BANNED            (terminal)
 * </pre>
 *
 * <p><strong>REFUSED and BANNED are both terminal in this implementation</strong> — no
 * {@code reapply()} operation exists. This is a confirmed project decision that deviates from
 * the literal README sentence "a refused candidacy allows the candidate to reapply", made to
 * follow the FSM diagram instead (which draws {@code Declined} straight into a terminal state).
 * See {@link SupplierStatus} javadoc and {@code SOLUTION.md} for the full rationale — flag this
 * explicitly in the interview.
 *
 * <p><strong>Extension beyond the current OpenAPI contract:</strong> {@link #restrict()} and
 * {@link #promote()} model the diagram-only {@code Active --Restrict--> On Probation} and
 * {@code On Probation --Promote--> Active} edges. No controller currently exposes them (the
 * OpenAPI spec has no corresponding endpoint) — they exist as forward-looking stubs. See
 * {@code SOLUTION.md}.
 *
 * <p><strong>Acceptance guards</strong> ({@link #accept}): a candidate cannot be accepted if
 * its country is on the non-approved countries list (checked by the caller via
 * {@code application.port.out.CountryCheckPort} — the aggregate itself has no I/O) or if
 * {@code annualTurnover < 1,000,000}. Country-check unavailability (circuit breaker open) must
 * fail safe: never assume a country is *not* banned when the check fails
 * ({@code CountryCheckUnavailableException}, mapped to the same 409 as
 * {@link CandidateNotAcceptableException}).
 */
public class SupplierRecord {

    private final Duns duns;
    private String name;
    private CountryCode country;
    private AnnualTurnover annualTurnover;
    private SupplierStatus status;
    private SustainabilityRating sustainabilityRating;

    private SupplierRecord(Duns duns, String name, CountryCode country, AnnualTurnover annualTurnover,
                            SupplierStatus status, SustainabilityRating sustainabilityRating) {
        this.duns = duns;
        this.name = name;
        this.country = country;
        this.annualTurnover = annualTurnover;
        this.status = status;
        this.sustainabilityRating = sustainabilityRating;
    }

    /**
     * Factory for a brand-new candidate application ({@code POST /candidates}).
     *
     * <p>Callers (the application service) are responsible for first checking, via
     * {@code SupplierRepositoryPort}, that no {@link SupplierRecord} already exists for this
     * {@link Duns}: if one does, throw {@link CandidateAlreadyExistsException} (any non-BANNED
     * status) or {@link SupplierBannedException} (BANNED status) *before* calling this factory.
     * This method itself performs no uniqueness check — it only builds a valid new aggregate in
     * {@link SupplierStatus#CANDIDATE}.
     *
     * @throws IllegalArgumentException if any field is invalid (delegated to the value objects)
     */
    public static SupplierRecord apply(Duns duns, String name, CountryCode country, AnnualTurnover annualTurnover) {
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * Reconstitutes a {@link SupplierRecord} from persisted state. Used exclusively by
     * {@code infrastructure.persistence.mapper} — never call this from application services.
     */
    public static SupplierRecord reconstitute(Duns duns, String name, CountryCode country,
                                               AnnualTurnover annualTurnover, SupplierStatus status,
                                               SustainabilityRating sustainabilityRating) {
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * Accepts a pending candidate, assigning the initial sustainability rating.
     *
     * <p>Guards (in the order they should be checked, so the most specific exception wins):
     * <ol>
     *   <li>{@code status != CANDIDATE} → {@link CandidateNotAcceptableException}</li>
     *   <li>country on the non-approved list → {@link CandidateNotAcceptableException}
     *       (the caller resolves this via {@code CountryCheckPort} and passes the boolean/result
     *       in, or this method receives a pre-resolved verdict — decide and document the chosen
     *       shape when implementing)</li>
     *   <li>{@code annualTurnover < 1,000,000} → {@link CandidateNotAcceptableException}</li>
     * </ol>
     * On success: {@code rating} A/B → {@link SupplierStatus#ACTIVE};
     * C/D/E → {@link SupplierStatus#ON_PROBATION}.
     *
     * @param rating the initial sustainability rating assigned by the supervisor
     * @param countryBanned pre-resolved result of the country check (fail-safe: true if the
     *     check could not be completed — see {@code CountryCheckUnavailableException})
     */
    public void accept(SustainabilityRating rating, boolean countryBanned) {
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * Refuses a pending candidate.
     *
     * @throws CandidateNotRefusableException if {@code status != CANDIDATE}
     */
    public void refuse() {
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * Bans a supplier on probation. Per confirmed project decision, valid only from
     * {@link SupplierStatus#ON_PROBATION} — <strong>not</strong> from {@link SupplierStatus#ACTIVE}.
     *
     * @throws SupplierNotBannableException if {@code status != ON_PROBATION}
     */
    public void ban() {
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * Extension stub (no OpenAPI endpoint yet, see class javadoc): demotes an active supplier to
     * probation, mirroring the diagram's {@code Restrict} edge.
     *
     * @throws com.inditex.supplier.domain.exception.SupplierNotRestrictableException if
     *     {@code status != ACTIVE}
     */
    public void restrict() {
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * Extension stub (no OpenAPI endpoint yet, see class javadoc): promotes a supplier on
     * probation to active, mirroring the diagram's {@code Promote} edge.
     *
     * @throws com.inditex.supplier.domain.exception.SupplierNotPromotableException if
     *     {@code status != ON_PROBATION}
     */
    public void promote() {
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * @return true if this record's current status is one visible through the
     *     {@code /candidates/{duns}} resource, i.e. {@code CANDIDATE} or {@code REFUSED}
     *     (see {@code SOLUTION.md} §"API pública vs. estado interno").
     */
    public boolean isVisibleAsCandidate() {
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * @return true if this record's current status is one visible through the
     *     {@code /suppliers/{duns}} resource, i.e. {@code ACTIVE}, {@code ON_PROBATION} or
     *     {@code BANNED}.
     */
    public boolean isVisibleAsSupplier() {
        throw new UnsupportedOperationException("TODO");
    }

    public Duns duns() {
        return duns;
    }

    public String name() {
        return name;
    }

    public CountryCode country() {
        return country;
    }

    public AnnualTurnover annualTurnover() {
        return annualTurnover;
    }

    public SupplierStatus status() {
        return status;
    }

    public SustainabilityRating sustainabilityRating() {
        return sustainabilityRating;
    }
}
