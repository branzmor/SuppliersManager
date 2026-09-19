package com.supplier.management.infrastructure.web.mapper;

import com.supplier.management.application.port.out.ScoredSupplier;
import com.supplier.management.domain.model.SupplierRecord;
import com.supplier.management.domain.model.SupplierStatus;
import com.supplier.management.domain.model.SustainabilityRating;
import com.supplier.management.infrastructure.web.dto.PotentialSupplierResponseDto;
import com.supplier.management.infrastructure.web.dto.SupplierResponseDto;
import com.supplier.management.infrastructure.web.dto.SupplierStatusDto;
import com.supplier.management.infrastructure.web.dto.SustainabilityRatingDto;
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
        return new SupplierResponseDto(
                record.annualTurnover().value(),
                record.country().isoCode(),
                record.duns().value(),
                record.name(),
                toStatusDto(record.status()),
                SustainabilityRatingDto.valueOf(record.sustainabilityRating().name()));
    }

    public PotentialSupplierResponseDto toPotentialResponseDto(ScoredSupplier scoredSupplier) {
        SupplierRecord record = scoredSupplier.record();
        return new PotentialSupplierResponseDto(
                record.annualTurnover().value(),
                record.country().isoCode(),
                record.duns().value(),
                record.name(),
                toStatusDto(record.status()),
                SustainabilityRatingDto.valueOf(record.sustainabilityRating().name()),
                scoredSupplier.score());
    }

    /**
     * @throws IllegalStateException if {@code status} is {@code CANDIDATE} or {@code REFUSED} —
     *     those must never reach this mapper.
     */
    public SupplierStatusDto toStatusDto(SupplierStatus status) {
        return switch (status) {
            case ACTIVE, ON_PROBATION -> SupplierStatusDto.Active;
            case BANNED -> SupplierStatusDto.Disqualified;
            case CANDIDATE, REFUSED -> throw new IllegalStateException(
                    "status " + status + " must never reach SupplierWebMapper - isVisibleAsSupplier() should have filtered it out upstream");
        };
    }
}
