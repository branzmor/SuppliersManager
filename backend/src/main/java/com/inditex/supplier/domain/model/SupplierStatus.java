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
 *   ON_PROBATION --ban()--&gt;         BANNED
 * </pre>
 *
 * <p><strong>Project decision (deviates from the literal README text "a refused candidacy
 * allows the candidate to reapply"):</strong> per the FSM diagram
 * ({@code wiki/iop-techtest-fsm-supplier.png}), which draws {@code Declined} flowing directly
 * into a terminal state with no edge back to {@code Candidate}, this implementation treats
 * {@link #REFUSED} as terminal, exactly like {@link #BANNED}. There is no {@code reapply()}
 * operation. A new {@code POST /candidates} for a DUNS already in {@code REFUSED} status is
 * rejected with {@code CandidateAlreadyExistsException}, the same as any other non-{@code BANNED}
 * existing record. This is documented as an explicit, user-confirmed deviation in
 * {@code SOLUTION.md} — bring it up in the interview.
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
     * @return true if no transition is possible out of this status ({@link #REFUSED} and
     *     {@link #BANNED}).
     */
    public boolean isTerminal() {
        throw new UnsupportedOperationException("TODO");
    }
}
