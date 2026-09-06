package com.inditex.supplier.domain.model;

/**
 * Internal status of a {@link SupplierRecord} — the 5 states of the FSM.
 *
 * <p>This is <strong>not</strong> the same as the {@code status} field exposed by the public API
 * (which only knows {@code Active}/{@code Disqualified}). The internal→external mapping lives
 * exclusively in {@code infrastructure.web.mapper} (see {@code SOLUTION.md} §"API pública vs.
 * estado interno").
 *
 * <p><strong>Transitions (see {@link SupplierRecord} javadoc for the full FSM contract):</strong>
 * <pre>
 *   CANDIDATE --accept(A|B)--&gt;      ACTIVE
 *   CANDIDATE --accept(C|D|E)--&gt;    ON_PROBATION
 *   CANDIDATE --refuse()--&gt;         REFUSED
 *   REFUSED --reapply(...)--&gt;       CANDIDATE
 *   ON_PROBATION --ban()--&gt;         BANNED
 * </pre>
 *
 * <p><strong>{@link #REFUSED} is not terminal — a refused candidacy can reapply</strong>, per the
 * README business text ("a refused candidacy allows the candidate to reapply"). {@code POST
 * /candidates} for a DUNS currently in {@code REFUSED} status calls
 * {@code SupplierRecord#reapply}, which updates the mutable fields, clears any previous rating,
 * and moves the record back to {@link #CANDIDATE}. Only {@link #BANNED} is terminal — a banned
 * supplier can never become a candidate or supplier again for that DUNS. See
 * {@code SupplierRecord#reapply} javadoc and {@code SOLUTION.md} for the full rationale.
 *
 * <p><strong>Extension beyond the current OpenAPI contract:</strong> the FSM diagram also draws
 * {@code Active --Restrict--> On Probation} and {@code On Probation --Promote--> Active}, which
 * are not covered by the README business text nor by any endpoint in
 * {@code itx-iop_tech-supplier_flow-main-openapi3_1.yaml}. Per project decision, {@code restrict()}
 * and {@code promote()} exist as domain/application stubs (no controller wiring yet) so the
 * shape is ready if/when the contract is extended. See {@code SOLUTION.md}.
 */
public enum SupplierStatus {
    CANDIDATE,
    ACTIVE,
    ON_PROBATION,
    REFUSED,
    BANNED;

    /**
     * @return true if no transition is possible out of this status. Only {@link #BANNED} is
     *     terminal — {@link #REFUSED} can still transition back to {@link #CANDIDATE} via
     *     {@code SupplierRecord#reapply}.
     */
    public boolean isTerminal() {
        return this == BANNED;
    }
}
