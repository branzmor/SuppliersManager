package com.inditex.supplier.infrastructure.web.mapper;

import com.inditex.supplier.application.port.in.RegisterCandidateUseCase.RegisterCandidateCommand;
import com.inditex.supplier.domain.model.AnnualTurnover;
import com.inditex.supplier.domain.model.CountryCode;
import com.inditex.supplier.domain.model.Duns;
import com.inditex.supplier.domain.model.SupplierRecord;
import com.inditex.supplier.infrastructure.web.dto.CandidateRequestDto;
import com.inditex.supplier.infrastructure.web.dto.CandidateResponseDto;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Request DTO → command carries all 4 fields verbatim; domain record → response DTO carries all
 * 4 {@code Candidate} schema fields, and — important — does NOT include {@code status} (the
 * {@code Candidate} schema has no status field, only {@code Supplier} does — enforced here simply
 * by {@link CandidateResponseDto} having no such field to assert against).
 */
class CandidateWebMapperTest {

    private final CandidateWebMapper mapper = new CandidateWebMapper();

    @Test
    void mapsRequestDtoToCommand() {
        CandidateRequestDto dto = new CandidateRequestDto(2_000_000L, "ES", 123_456_789, "Zippers & Buttons");

        RegisterCandidateCommand command = mapper.toCommand(dto);

        assertThat(command.duns()).isEqualTo(123_456_789);
        assertThat(command.name()).isEqualTo("Zippers & Buttons");
        assertThat(command.country()).isEqualTo("ES");
        assertThat(command.annualTurnover()).isEqualTo(2_000_000L);
    }

    @Test
    void mapsDomainRecordToResponseDto() {
        SupplierRecord record = SupplierRecord.apply(new Duns(123_456_789), "Zippers & Buttons",
                new CountryCode("ES"), new AnnualTurnover(2_000_000L));

        CandidateResponseDto dto = mapper.toResponseDto(record);

        assertThat(dto.duns()).isEqualTo(123_456_789);
        assertThat(dto.name()).isEqualTo("Zippers & Buttons");
        assertThat(dto.country()).isEqualTo("ES");
        assertThat(dto.annualTurnover()).isEqualTo(2_000_000L);
    }
}
