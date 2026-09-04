package com.inditex.supplier.infrastructure.web.mapper;

import com.inditex.supplier.application.port.out.ScoredSupplier;
import com.inditex.supplier.domain.model.AnnualTurnover;
import com.inditex.supplier.domain.model.CountryCode;
import com.inditex.supplier.domain.model.Duns;
import com.inditex.supplier.domain.model.SupplierRecord;
import com.inditex.supplier.domain.model.SupplierStatus;
import com.inditex.supplier.domain.model.SustainabilityRating;
import com.inditex.supplier.infrastructure.web.dto.PotentialSupplierResponseDto;
import com.inditex.supplier.infrastructure.web.dto.SupplierResponseDto;
import com.inditex.supplier.infrastructure.web.dto.SupplierStatusDto;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

/**
 * The single most important web-layer test, since it's the only place the internal→external
 * status mapping is allowed to happen (see {@code SOLUTION.md} §"API pública vs. estado interno"):
 * <ul>
 *   <li>{@code ACTIVE} → {@code Active}.</li>
 *   <li>{@code ON_PROBATION} → {@code Active} (not exposed distinctly, per README §"Integrity Rules").</li>
 *   <li>{@code BANNED} → {@code Disqualified}.</li>
 *   <li>{@code CANDIDATE} or {@code REFUSED} passed in → defends with {@code IllegalStateException}.</li>
 *   <li>{@code toPotentialResponseDto} carries the {@code score} field through unchanged.</li>
 * </ul>
 */
class SupplierWebMapperTest {

    private final SupplierWebMapper mapper = new SupplierWebMapper();

    private static SupplierRecord recordIn(SupplierStatus status) {
        SustainabilityRating rating = status == SupplierStatus.CANDIDATE || status == SupplierStatus.REFUSED
                ? null : SustainabilityRating.B;
        return SupplierRecord.reconstitute(new Duns(123_456_789), "Zippers & Buttons", new CountryCode("ES"),
                new AnnualTurnover(2_000_000L), status, rating);
    }

    @Test
    void mapsActiveAndOnProbationToActiveStatusDto() {
        SupplierResponseDto activeDto = mapper.toResponseDto(recordIn(SupplierStatus.ACTIVE));
        assertThat(activeDto.status()).isEqualTo(SupplierStatusDto.Active);

        SupplierResponseDto onProbationDto = mapper.toResponseDto(recordIn(SupplierStatus.ON_PROBATION));
        assertThat(onProbationDto.status()).isEqualTo(SupplierStatusDto.Active);
    }

    @Test
    void mapsBannedToDisqualifiedStatusDto() {
        SupplierResponseDto dto = mapper.toResponseDto(recordIn(SupplierStatus.BANNED));
        assertThat(dto.status()).isEqualTo(SupplierStatusDto.Disqualified);
    }

    @Test
    void defendsAgainstCandidateOrRefusedStatus() {
        assertThatIllegalStateException().isThrownBy(() -> mapper.toStatusDto(SupplierStatus.CANDIDATE));
        assertThatIllegalStateException().isThrownBy(() -> mapper.toStatusDto(SupplierStatus.REFUSED));
    }

    @Test
    void potentialSupplierMappingCarriesScoreThrough() {
        ScoredSupplier scoredSupplier = new ScoredSupplier(recordIn(SupplierStatus.ACTIVE), 375_000.0);

        PotentialSupplierResponseDto dto = mapper.toPotentialResponseDto(scoredSupplier);

        assertThat(dto.score()).isEqualTo(375_000.0);
        assertThat(dto.status()).isEqualTo(SupplierStatusDto.Active);
        assertThat(dto.duns()).isEqualTo(123_456_789);
    }
}
