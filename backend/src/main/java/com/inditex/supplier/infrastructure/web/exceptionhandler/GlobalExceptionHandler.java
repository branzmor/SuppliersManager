package com.inditex.supplier.infrastructure.web.exceptionhandler;

import com.inditex.supplier.domain.exception.CandidateAlreadyExistsException;
import com.inditex.supplier.domain.exception.CandidateNotAcceptableException;
import com.inditex.supplier.domain.exception.CandidateNotRefusableException;
import com.inditex.supplier.domain.exception.CountryCheckUnavailableException;
import com.inditex.supplier.domain.exception.SupplierBannedException;
import com.inditex.supplier.domain.exception.SupplierNotBannableException;
import com.inditex.supplier.domain.exception.SupplierRecordNotFoundException;
import com.inditex.supplier.infrastructure.web.dto.ErrorResponseDto;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

/**
 * Translates domain exceptions (and Bean Validation failures) into the HTTP responses defined by
 * the OpenAPI contract.
 *
 * <table>
 *   <caption>Exception -&gt; HTTP mapping</caption>
 *   <tr><th>Exception</th><th>When</th><th>HTTP</th></tr>
 *   <tr><td>{@link CandidateAlreadyExistsException}</td>
 *       <td>Active candidacy or supplier already exists for the DUNS (includes REFUSED, per the
 *           no-reapply decision)</td><td>409 "Candidate already exists"</td></tr>
 *   <tr><td>{@link SupplierBannedException}</td>
 *       <td>{@code POST /candidates} on a DUNS in BANNED status</td>
 *       <td>409 "Supplier banned"</td></tr>
 *   <tr><td>{@link CandidateNotAcceptableException}</td>
 *       <td>status != CANDIDATE, country not approved, or turnover &lt; 1M</td>
 *       <td>409 "Candidate can not be accepted"</td></tr>
 *   <tr><td>{@link CandidateNotRefusableException}</td>
 *       <td>status != CANDIDATE</td><td>409 "Candidate can not be refused"</td></tr>
 *   <tr><td>{@link SupplierNotBannableException}</td>
 *       <td>status != ON_PROBATION</td><td>409 "Supplier can not be banned"</td></tr>
 *   <tr><td>{@link CountryCheckUnavailableException}</td>
 *       <td>Circuit breaker open / country service failure</td>
 *       <td>409, reuses {@link CandidateNotAcceptableException}'s message (fail-safe: never
 *           assume a country is not banned on failure)</td></tr>
 *   <tr><td>{@link SupplierRecordNotFoundException}</td>
 *       <td>No record exists for the DUNS on accept/refuse/ban</td><td>404 (no body)</td></tr>
 *   <tr><td>{@link MethodArgumentNotValidException}</td>
 *       <td>Bean Validation failure on a request DTO</td><td>400</td></tr>
 * </table>
 *
 * <p><strong>Known gap (documented, not implemented):</strong> the OpenAPI declares
 * {@code 422 Unprocessable Content} on {@code POST /candidates}, but no business rule in the
 * README triggers it distinctly from 400 (invalid schema) or 409 (duplicate/banned). See
 * {@code SOLUTION.md} §"Aspectos dejados fuera y por qué".
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(CandidateAlreadyExistsException.class)
    public ResponseEntity<ErrorResponseDto> handleCandidateAlreadyExists(CandidateAlreadyExistsException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(new ErrorResponseDto("Candidate already exists"));
    }

    @ExceptionHandler(SupplierBannedException.class)
    public ResponseEntity<ErrorResponseDto> handleSupplierBanned(SupplierBannedException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(new ErrorResponseDto("Supplier banned"));
    }

    @ExceptionHandler(CandidateNotAcceptableException.class)
    public ResponseEntity<ErrorResponseDto> handleCandidateNotAcceptable(CandidateNotAcceptableException ex) {
        throw new UnsupportedOperationException("TODO");
    }

    @ExceptionHandler(CandidateNotRefusableException.class)
    public ResponseEntity<ErrorResponseDto> handleCandidateNotRefusable(CandidateNotRefusableException ex) {
        throw new UnsupportedOperationException("TODO");
    }

    @ExceptionHandler(SupplierNotBannableException.class)
    public ResponseEntity<ErrorResponseDto> handleSupplierNotBannable(SupplierNotBannableException ex) {
        throw new UnsupportedOperationException("TODO");
    }

    @ExceptionHandler(CountryCheckUnavailableException.class)
    public ResponseEntity<ErrorResponseDto> handleCountryCheckUnavailable(CountryCheckUnavailableException ex) {
        // TODO: fail-safe — respond exactly like CandidateNotAcceptableException (409, same message).
        throw new UnsupportedOperationException("TODO");
    }

    @ExceptionHandler(SupplierRecordNotFoundException.class)
    public ResponseEntity<Void> handleSupplierRecordNotFound(SupplierRecordNotFoundException ex) {
        throw new UnsupportedOperationException("TODO");
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponseDto> handleValidation(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + " " + error.getDefaultMessage())
                .collect(Collectors.joining(", "));
        return ResponseEntity.badRequest().body(new ErrorResponseDto(message));
    }

    /**
     * Catches domain value-object validation that Bean Validation on the DTO doesn't fully
     * replicate — e.g. {@code CountryCode} requires exactly 2 uppercase letters, while the DTO
     * only checks length. Not part of the original 6-exception table; added so a request that
     * passes DTO validation but fails a domain invariant still gets a 400, not a 500.
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponseDto> handleIllegalArgument(IllegalArgumentException ex) {
        return ResponseEntity.badRequest().body(new ErrorResponseDto(ex.getMessage()));
    }
}
