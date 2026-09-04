package com.inditex.supplier.infrastructure.web.mapper;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

/**
 * TODO coverage — the single most important web-layer test, since it's the only place the
 * internal→external status mapping is allowed to happen (see {@code SOLUTION.md}
 * §"API pública vs. estado interno"):
 * <ul>
 *   <li>{@code ACTIVE} → {@code Active}.</li>
 *   <li>{@code ON_PROBATION} → {@code Active} (not exposed distinctly, per README §"Integrity Rules").</li>
 *   <li>{@code BANNED} → {@code Disqualified}.</li>
 *   <li>{@code CANDIDATE} or {@code REFUSED} passed in → should never happen (upstream
 *       {@code isVisibleAsSupplier} filter); assert this mapper defends against it
 *       (e.g. throws {@code IllegalStateException}) rather than silently mapping.</li>
 *   <li>{@code toPotentialResponseDto} carries the {@code score} field through unchanged.</li>
 * </ul>
 */
class SupplierWebMapperTest {

    @Test
    @Disabled("TODO: implement - see class javadoc")
    void mapsActiveAndOnProbationToActiveStatusDto() {
    }

    @Test
    @Disabled("TODO: implement - see class javadoc")
    void mapsBannedToDisqualifiedStatusDto() {
    }

    @Test
    @Disabled("TODO: implement - see class javadoc")
    void defendsAgainstCandidateOrRefusedStatus() {
    }

    @Test
    @Disabled("TODO: implement - see class javadoc")
    void potentialSupplierMappingCarriesScoreThrough() {
    }
}
