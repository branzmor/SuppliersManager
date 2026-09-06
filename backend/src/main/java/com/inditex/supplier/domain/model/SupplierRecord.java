package com.inditex.supplier.domain.model;

import com.inditex.supplier.domain.exception.CandidateAlreadyExistsException;
import com.inditex.supplier.domain.exception.CandidateNotAcceptableException;
import com.inditex.supplier.domain.exception.CandidateNotRefusableException;
import com.inditex.supplier.domain.exception.SupplierBannedException;
import com.inditex.supplier.domain.exception.SupplierNotBannableException;
import com.inditex.supplier.domain.exception.SupplierNotPromotableException;
import com.inditex.supplier.domain.exception.SupplierNotRestrictableException;

import java.util.Objects;

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
 *   CANDIDATE --refuse()--&gt;             REFUSED
 *   REFUSED --reapply(...)--&gt;           CANDIDATE
 *   ON_PROBATION --ban()--&gt;             BANNED            (terminal)
 * </pre>
 *
 * <p><strong>Only BANNED is terminal.</strong> Per the README business text ("a refused
 * candidacy allows the candidate to reapply"), {@link #reapply} lets a {@code REFUSED} record
 * become a fresh {@code CANDIDATE} again, updating its mutable fields and clearing any previous
 * rating. See {@link SupplierStatus} javadoc and {@code SOLUTION.md} for the full rationale.
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
     * {@code SupplierRepositoryPort}, whether a {@link SupplierRecord} already exists for this
     * {@link Duns}:
     * <ul>
     *   <li>no existing record → call this factory;</li>
     *   <li>existing record in {@link SupplierStatus#REFUSED} → call {@link #reapply} on it
     *       instead, do not call this factory;</li>
     *   <li>existing record in {@link SupplierStatus#BANNED} → throw
     *       {@link SupplierBannedException};</li>
     *   <li>existing record in any other status → throw {@link CandidateAlreadyExistsException}.</li>
     * </ul>
     * This method itself performs no uniqueness check — it only builds a valid new aggregate in
     * {@link SupplierStatus#CANDIDATE}.
     *
     * @throws IllegalArgumentException if any field is invalid (delegated to the value objects)
     */
    public static SupplierRecord apply(Duns duns, String name, CountryCode country, AnnualTurnover annualTurnover) {
        Objects.requireNonNull(duns, "duns must not be null");
        Objects.requireNonNull(country, "country must not be null");
        Objects.requireNonNull(annualTurnover, "annualTurnover must not be null");
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("name must not be blank");
        }
        return new SupplierRecord(duns, name, country, annualTurnover, SupplierStatus.CANDIDATE, null);
    }

    /**
     * Reconstitutes a {@link SupplierRecord} from persisted state. Used exclusively by
     * {@code infrastructure.persistence.mapper} — never call this from application services.
     */
    public static SupplierRecord reconstitute(Duns duns, String name, CountryCode country,
                                               AnnualTurnover annualTurnover, SupplierStatus status,
                                               SustainabilityRating sustainabilityRating) {
        Objects.requireNonNull(duns, "duns must not be null");
        Objects.requireNonNull(name, "name must not be null");
        Objects.requireNonNull(country, "country must not be null");
        Objects.requireNonNull(annualTurnover, "annualTurnover must not be null");
        Objects.requireNonNull(status, "status must not be null");
        return new SupplierRecord(duns, name, country, annualTurnover, status, sustainabilityRating);
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
        Objects.requireNonNull(rating, "rating must not be null");
        if (status != SupplierStatus.CANDIDATE) {
            throw new CandidateNotAcceptableException(duns, "status is " + status + ", expected CANDIDATE");
        }
        if (countryBanned) {
            throw new CandidateNotAcceptableException(duns, "country " + country.isoCode() + " is not approved");
        }
        if (!annualTurnover.meetsMinimumForAcceptance()) {
            throw new CandidateNotAcceptableException(duns, "annual turnover " + annualTurnover.value()
                    + " is below the minimum of " + AnnualTurnover.MINIMUM_ACCEPTABLE_TURNOVER);
        }
        this.sustainabilityRating = rating;
        this.status = rating.qualifiesForActive() ? SupplierStatus.ACTIVE : SupplierStatus.ON_PROBATION;
    }

    /**
     * Refuses a pending candidate.
     *
     * @throws CandidateNotRefusableException if {@code status != CANDIDATE}
     */
    public void refuse() {
        if (status != SupplierStatus.CANDIDATE) {
            throw new CandidateNotRefusableException(duns);
        }
        this.status = SupplierStatus.REFUSED;
    }

    /**
     * Lets a previously refused candidacy reapply ({@code POST /candidates} on a DUNS whose
     * current status is {@link SupplierStatus#REFUSED}), per the README business rule "a refused
     * candidacy allows the candidate to reapply".
     *
     * <p>Replaces {@code name}/{@code country}/{@code annualTurnover} with the newly submitted
     * values, discards any previous {@code sustainabilityRating} (a reapplication is a fresh
     * candidacy, not a resumption of the old one), and moves the record back to
     * {@link SupplierStatus#CANDIDATE}.
     *
     * <p>The caller (the application service) is responsible for only invoking this method when
     * {@code status == REFUSED} — it is the counterpart to {@link #apply}'s "no existing record"
     * branch. The guard below is defense-in-depth, not the primary check.
     *
     * @throws IllegalStateException if {@code status != REFUSED}
     * @throws IllegalArgumentException if any field is invalid (delegated to the value objects)
     */
    public void reapply(String name, CountryCode country, AnnualTurnover annualTurnover) {
        if (status != SupplierStatus.REFUSED) {
            throw new IllegalStateException("reapply is only valid from REFUSED, was " + status);
        }
        Objects.requireNonNull(country, "country must not be null");
        Objects.requireNonNull(annualTurnover, "annualTurnover must not be null");
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("name must not be blank");
        }
        this.name = name;
        this.country = country;
        this.annualTurnover = annualTurnover;
        this.sustainabilityRating = null;
        this.status = SupplierStatus.CANDIDATE;
    }

    /**
     * Bans a supplier on probation. Per confirmed project decision, valid only from
     * {@link SupplierStatus#ON_PROBATION} — <strong>not</strong> from {@link SupplierStatus#ACTIVE}.
     *
     * @throws SupplierNotBannableException if {@code status != ON_PROBATION}
     */
    public void ban() {
        if (status != SupplierStatus.ON_PROBATION) {
            throw new SupplierNotBannableException(duns);
        }
        this.status = SupplierStatus.BANNED;
    }

    /**
     * Extension stub (no OpenAPI endpoint yet, see class javadoc): demotes an active supplier to
     * probation, mirroring the diagram's {@code Restrict} edge.
     *
     * @throws com.inditex.supplier.domain.exception.SupplierNotRestrictableException if
     *     {@code status != ACTIVE}
     */
    public void restrict() {
        if (status != SupplierStatus.ACTIVE) {
            throw new SupplierNotRestrictableException(duns);
        }
        this.status = SupplierStatus.ON_PROBATION;
    }

    /**
     * Extension stub (no OpenAPI endpoint yet, see class javadoc): promotes a supplier on
     * probation to active, mirroring the diagram's {@code Promote} edge.
     *
     * @throws com.inditex.supplier.domain.exception.SupplierNotPromotableException if
     *     {@code status != ON_PROBATION}
     */
    public void promote() {
        if (status != SupplierStatus.ON_PROBATION) {
            throw new SupplierNotPromotableException(duns);
        }
        this.status = SupplierStatus.ACTIVE;
    }

    /**
     * @return true if this record's current status is one visible through the
     *     {@code /candidates/{duns}} resource, i.e. {@code CANDIDATE} or {@code REFUSED}
     *     (see {@code SOLUTION.md} §"API pública vs. estado interno").
     */
    public boolean isVisibleAsCandidate() {
        return status == SupplierStatus.CANDIDATE || status == SupplierStatus.REFUSED;
    }

    /**
     * @return true if this record's current status is one visible through the
     *     {@code /suppliers/{duns}} resource, i.e. {@code ACTIVE}, {@code ON_PROBATION} or
     *     {@code BANNED}.
     */
    public boolean isVisibleAsSupplier() {
        return status == SupplierStatus.ACTIVE || status == SupplierStatus.ON_PROBATION || status == SupplierStatus.BANNED;
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
