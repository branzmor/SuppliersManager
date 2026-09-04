package com.inditex.supplier.infrastructure.web.mapper;

import com.inditex.supplier.application.port.in.RegisterCandidateUseCase.RegisterCandidateCommand;
import com.inditex.supplier.domain.model.SupplierRecord;
import com.inditex.supplier.infrastructure.web.dto.CandidateRequestDto;
import com.inditex.supplier.infrastructure.web.dto.CandidateResponseDto;
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
        throw new UnsupportedOperationException("TODO");
    }

    public CandidateResponseDto toResponseDto(SupplierRecord record) {
        throw new UnsupportedOperationException("TODO");
    }
}
