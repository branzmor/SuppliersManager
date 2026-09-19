package com.supplier.management.infrastructure.web.controller;

import com.supplier.management.application.port.in.BanSupplierUseCase;
import com.supplier.management.application.port.in.GetPotentialSuppliersUseCase;
import com.supplier.management.application.port.in.GetSupplierUseCase;
import com.supplier.management.domain.exception.SupplierRecordNotFoundException;
import com.supplier.management.domain.model.Duns;
import com.supplier.management.infrastructure.web.dto.PaginationDto;
import com.supplier.management.infrastructure.web.dto.PotentialSupplierResponseDto;
import com.supplier.management.infrastructure.web.dto.PotentialSuppliersResponseDto;
import com.supplier.management.infrastructure.web.dto.SupplierResponseDto;
import com.supplier.management.infrastructure.web.mapper.SupplierWebMapper;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.validation.annotation.Validated;

import java.util.List;

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
        GetPotentialSuppliersUseCase.Result result = getPotentialSuppliersUseCase.getPotentialSuppliers(
                new GetPotentialSuppliersUseCase.Query(rate, limit, offset));
        List<PotentialSupplierResponseDto> data = result.suppliers().stream()
                .map(supplierWebMapper::toPotentialResponseDto)
                .toList();
        PaginationDto pagination = new PaginationDto(result.limit(), result.offset(), (int) result.total());
        return new PotentialSuppliersResponseDto(data, pagination);
    }

    @GetMapping("/{duns}")
    public SupplierResponseDto getSupplier(@PathVariable int duns) {
        return getSupplierUseCase.getByDuns(duns)
                .map(supplierWebMapper::toResponseDto)
                .orElseThrow(() -> new SupplierRecordNotFoundException(new Duns(duns)));
    }

    @PostMapping("/{duns}/ban")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void banSupplier(@PathVariable int duns) {
        banSupplierUseCase.ban(duns);
    }
}
