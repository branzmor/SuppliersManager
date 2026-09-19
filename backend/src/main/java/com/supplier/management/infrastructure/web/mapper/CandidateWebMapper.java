package com.supplier.management.infrastructure.web.mapper;

import com.supplier.management.application.port.in.RegisterCandidateUseCase.RegisterCandidateCommand;
import com.supplier.management.domain.model.SupplierRecord;
import com.supplier.management.infrastructure.web.dto.CandidateRequestDto;
import com.supplier.management.infrastructure.web.dto.CandidateResponseDto;
import org.springframework.stereotype.Component;

/**
 * Maps between {@code CandidateRequestDto}/{@code CandidateResponseDto} and the domain.
 *
 * <p>No status mapping happens here — the {@code Candidate} schema has no {@code status} field.
 * See {@code SupplierWebMapper} for the {@code Supplier} schema's status mapping.
 */
@Component
public class CandidateWebMapper {

    public RegisterCandidateCommand toCommand(CandidateRequestDto dto) {
        return new RegisterCandidateCommand(dto.duns(), dto.name(), dto.country(), dto.annualTurnover());
    }

    public CandidateResponseDto toResponseDto(SupplierRecord record) {
        return new CandidateResponseDto(
                record.annualTurnover().value(), record.country().isoCode(), record.duns().value(), record.name());
    }
}
