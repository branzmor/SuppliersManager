package com.inditex.supplier.infrastructure.web.controller;

import com.inditex.supplier.application.port.in.BanSupplierUseCase;
import com.inditex.supplier.application.port.in.GetPotentialSuppliersUseCase;
import com.inditex.supplier.application.port.in.GetSupplierUseCase;
import com.inditex.supplier.infrastructure.web.dto.PotentialSuppliersResponseDto;
import com.inditex.supplier.infrastructure.web.dto.SupplierResponseDto;
import com.inditex.supplier.infrastructure.web.mapper.SupplierWebMapper;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.validation.annotation.Validated;

/**
 * REST controller for the {@code supplier} tag of the main OpenAPI spec.
 *
 * <p>Maps 1:1 to:
 * <ul>
 *   <li>{@code GET /suppliers/potential} ({@code potentialSuppliers}) → 200 / 400</li>
 *   <li>{@code GET /suppliers/{duns}} ({@code getSupplier}) → 200 / 404</li>
 *   <li>{@code POST /suppliers/{duns}/ban} ({@code banSupplier}) → 204 / 404 / 409</li>
 * </ul>
 * Exception-to-HTTP translation is delegated to
 * {@code infrastructure.web.exceptionhandler.GlobalExceptionHandler}.
 */
@RestController
@RequestMapping("/suppliers")
@Validated
public class SupplierController {

    private final GetPotentialSuppliersUseCase getPotentialSuppliersUseCase;
    private final GetSupplierUseCase getSupplierUseCase;
    private final BanSupplierUseCase banSupplierUseCase;
    private final SupplierWebMapper supplierWebMapper;

    public SupplierController(GetPotentialSuppliersUseCase getPotentialSuppliersUseCase,
                               GetSupplierUseCase getSupplierUseCase,
                               BanSupplierUseCase banSupplierUseCase,
                               SupplierWebMapper supplierWebMapper) {
        this.getPotentialSuppliersUseCase = getPotentialSuppliersUseCase;
        this.getSupplierUseCase = getSupplierUseCase;
        this.banSupplierUseCase = banSupplierUseCase;
        this.supplierWebMapper = supplierWebMapper;
    }

    @GetMapping("/potential")
    public PotentialSuppliersResponseDto potentialSuppliers(
            @RequestParam @Min(250) long rate,
            @RequestParam(required = false, defaultValue = "10") @Min(1) @Max(10) int limit,
            @RequestParam(required = false, defaultValue = "0") @Min(0) int offset) {
        // TODO: call getPotentialSuppliersUseCase, map ScoredSupplier list + pagination -> response DTO.
        throw new UnsupportedOperationException("TODO");
    }

    @GetMapping("/{duns}")
    public SupplierResponseDto getSupplier(@PathVariable int duns) {
        // TODO: call getSupplierUseCase; empty -> 404 (SupplierRecordNotFoundException), else map -> 200.
        throw new UnsupportedOperationException("TODO");
    }

    @PostMapping("/{duns}/ban")
    public void banSupplier(@PathVariable int duns) {
        // TODO: call banSupplierUseCase, return 204.
        throw new UnsupportedOperationException("TODO");
    }
}
