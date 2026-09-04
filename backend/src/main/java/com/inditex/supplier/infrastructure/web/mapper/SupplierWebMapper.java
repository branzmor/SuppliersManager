package com.inditex.supplier.infrastructure.web.mapper;

import com.inditex.supplier.application.port.out.ScoredSupplier;
import com.inditex.supplier.domain.model.SupplierRecord;
import com.inditex.supplier.domain.model.SupplierStatus;
import com.inditex.supplier.infrastructure.web.dto.PotentialSupplierResponseDto;
import com.inditex.supplier.infrastructure.web.dto.SupplierResponseDto;
import com.inditex.supplier.infrastructure.web.dto.SupplierStatusDto;
import org.springframework.stereotype.Component;

/**
 * Maps domain {@link SupplierRecord} (and {@link ScoredSupplier}) to the public
 * {@code Supplier}/{@code PotentialSupplier} DTOs.
 *
 * <p><strong>This is the only place allowed to perform the internal→external status mapping</strong>
 * (see {@code SOLUTION.md} §"API pública vs. estado interno"):
 * <pre>
 *   ACTIVE, ON_PROBATION  -&gt;  Active
 *   BANNED                -&gt;  Disqualified
 * </pre>
 * Never leak {@link SupplierStatus#CANDIDATE} or {@link SupplierStatus#REFUSED} through this
 * mapper — those statuses must never reach a {@code Supplier}/{@code PotentialSupplier} DTO
 * because {@code SupplierRecord#isVisibleAsSupplier} already filtered them out upstream.
 */
@Component
public class SupplierWebMapper {

    public SupplierResponseDto toResponseDto(SupplierRecord record) {
        throw new UnsupportedOperationException("TODO");
    }

    public PotentialSupplierResponseDto toPotentialResponseDto(ScoredSupplier scoredSupplier) {
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * @throws IllegalStateException if {@code status} is {@code CANDIDATE} or {@code REFUSED} —
     *     those must never reach this mapper.
     */
    public SupplierStatusDto toStatusDto(SupplierStatus status) {
        throw new UnsupportedOperationException("TODO");
    }
}
