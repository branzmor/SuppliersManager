package com.inditex.supplier.infrastructure.web.controller;

import com.inditex.supplier.application.port.in.AcceptCandidateUseCase;
import com.inditex.supplier.application.port.in.GetCandidateUseCase;
import com.inditex.supplier.application.port.in.RefuseCandidateUseCase;
import com.inditex.supplier.application.port.in.RegisterCandidateUseCase;
import com.inditex.supplier.infrastructure.web.dto.CandidateAcceptRequestDto;
import com.inditex.supplier.infrastructure.web.dto.CandidateRequestDto;
import com.inditex.supplier.infrastructure.web.dto.CandidateResponseDto;
import com.inditex.supplier.infrastructure.web.mapper.CandidateWebMapper;
import com.inditex.supplier.domain.model.SupplierRecord;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for the {@code candidate} tag of the main OpenAPI spec.
 *
 * <p>Maps 1:1 to:
 * <ul>
 *   <li>{@code POST /candidates} ({@code addCandidate}) → 201 / 400 / 409 / 422</li>
 *   <li>{@code GET /candidates/{duns}} ({@code getCandidate}) → 200 / 404</li>
 *   <li>{@code POST /candidates/{duns}/accept} ({@code acceptCandidate}) → 204 / 400 / 404 / 409</li>
 *   <li>{@code POST /candidates/{duns}/refuse} ({@code refuseCandidate}) → 204 / 404 / 409</li>
 * </ul>
 * Exception-to-HTTP translation is delegated to
 * {@code infrastructure.web.exceptionhandler.GlobalExceptionHandler} — this controller only
 * orchestrates use case calls and DTO mapping.
 */
@RestController
@RequestMapping("/candidates")
public class CandidateController {

    private final RegisterCandidateUseCase registerCandidateUseCase;
    private final GetCandidateUseCase getCandidateUseCase;
    private final AcceptCandidateUseCase acceptCandidateUseCase;
    private final RefuseCandidateUseCase refuseCandidateUseCase;
    private final CandidateWebMapper candidateWebMapper;

    public CandidateController(RegisterCandidateUseCase registerCandidateUseCase,
                                GetCandidateUseCase getCandidateUseCase,
                                AcceptCandidateUseCase acceptCandidateUseCase,
                                RefuseCandidateUseCase refuseCandidateUseCase,
                                CandidateWebMapper candidateWebMapper) {
        this.registerCandidateUseCase = registerCandidateUseCase;
        this.getCandidateUseCase = getCandidateUseCase;
        this.acceptCandidateUseCase = acceptCandidateUseCase;
        this.refuseCandidateUseCase = refuseCandidateUseCase;
        this.candidateWebMapper = candidateWebMapper;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CandidateResponseDto addCandidate(@Valid @RequestBody CandidateRequestDto request) {
        SupplierRecord registered = registerCandidateUseCase.register(candidateWebMapper.toCommand(request));
        return candidateWebMapper.toResponseDto(registered);
    }

    @GetMapping("/{duns}")
    public CandidateResponseDto getCandidate(@PathVariable int duns) {
        // TODO: call getCandidateUseCase; if empty, let GlobalExceptionHandler / a thrown
        // SupplierRecordNotFoundException produce 404; otherwise map to response DTO with 200.
        throw new UnsupportedOperationException("TODO");
    }

    @PostMapping("/{duns}/accept")
    public void acceptCandidate(@PathVariable int duns, @Valid @RequestBody CandidateAcceptRequestDto request) {
        // TODO: map DTO rating -> domain SustainabilityRating, call acceptCandidateUseCase, return 204.
        throw new UnsupportedOperationException("TODO");
    }

    @PostMapping("/{duns}/refuse")
    public void refuseCandidate(@PathVariable int duns) {
        // TODO: call refuseCandidateUseCase, return 204.
        throw new UnsupportedOperationException("TODO");
    }
}
