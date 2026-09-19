package com.supplier.management.infrastructure.web.exceptionhandler;

import com.supplier.management.domain.exception.CandidateAlreadyExistsException;
import com.supplier.management.domain.exception.CandidateNotAcceptableException;
import com.supplier.management.domain.exception.CandidateNotRefusableException;
import com.supplier.management.domain.exception.CountryCheckUnavailableException;
import com.supplier.management.domain.exception.SupplierBannedException;
import com.supplier.management.domain.exception.SupplierNotBannableException;
import com.supplier.management.domain.exception.SupplierRecordNotFoundException;
import com.supplier.management.infrastructure.web.dto.ErrorResponseDto;
import jakarta.validation.ConstraintViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
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
 *       <td>Active candidacy or supplier already exists for the DUNS (i.e. status is
 *           {@code CANDIDATE}, {@code ACTIVE}, or {@code ON_PROBATION}). A DUNS in
 *           {@code REFUSED} instead reapplies via {@code SupplierRecord#reapply} and returns
 *           201; a DUNS in {@code BANNED} triggers {@link SupplierBannedException}
 *           instead)</td><td>409 "Candidate already exists"</td></tr>
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
 *   <tr><td>{@link org.springframework.orm.ObjectOptimisticLockingFailureException}</td>
 *       <td>Concurrent modification of the same row (see {@code SupplierRecordEntity#version})</td>
 *       <td>409, generic "modified concurrently" message</td></tr>
 *   <tr><td>{@link MethodArgumentNotValidException}</td>
 *       <td>Bean Validation failure on a request DTO</td><td>400</td></tr>
 *   <tr><td>{@link org.springframework.http.converter.HttpMessageNotReadableException}</td>
 *       <td>Request body cannot be deserialized (e.g. an enum field outside its declared values)
 *           — never reaches Bean Validation</td><td>400</td></tr>
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
        return ResponseEntity.status(HttpStatus.CONFLICT).body(new ErrorResponseDto("Candidate can not be accepted"));
    }

    @ExceptionHandler(CandidateNotRefusableException.class)
    public ResponseEntity<ErrorResponseDto> handleCandidateNotRefusable(CandidateNotRefusableException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(new ErrorResponseDto("Candidate can not be refused"));
    }

    @ExceptionHandler(SupplierNotBannableException.class)
    public ResponseEntity<ErrorResponseDto> handleSupplierNotBannable(SupplierNotBannableException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(new ErrorResponseDto("Supplier can not be banned"));
    }

    @ExceptionHandler(CountryCheckUnavailableException.class)
    public ResponseEntity<ErrorResponseDto> handleCountryCheckUnavailable(CountryCheckUnavailableException ex) {
        // Defense in depth: AcceptCandidateService already catches this and treats it as a banned
        // country before it reaches the domain, but handle it here too in case it ever escapes
        // from elsewhere — same fail-safe response as CandidateNotAcceptableException.
        return ResponseEntity.status(HttpStatus.CONFLICT).body(new ErrorResponseDto("Candidate can not be accepted"));
    }

    @ExceptionHandler(SupplierRecordNotFoundException.class)
    public ResponseEntity<Void> handleSupplierRecordNotFound(SupplierRecordNotFoundException ex) {
        return ResponseEntity.notFound().build();
    }

    /**
     * Thrown when {@code @Version}-based optimistic locking (see
     * {@code SupplierRecordEntity#version}) detects that the row being updated (by {@code accept},
     * {@code refuse}, {@code ban}, or a reapply through {@code POST /candidates}) was already
     * changed by another transaction since it was read. Never a stack trace or internal detail —
     * just a 409 telling the caller to re-fetch and retry.
     */
    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    public ResponseEntity<ErrorResponseDto> handleOptimisticLockingFailure(ObjectOptimisticLockingFailureException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ErrorResponseDto("Supplier record was modified concurrently, please retry"));
    }

    /**
     * Thrown when the request body cannot be deserialized at all — most commonly an enum field
     * (e.g. {@code sustainabilityRating}) holding a value outside its declared set. Without this
     * handler the request never reaches Bean Validation (deserialization fails first), so it fell
     * through to Spring's default MVC error handling: still a 400, but with an empty/non-conforming
     * body instead of the {@code Error} schema every other 400 in this contract uses — a gap
     * surfaced by {@code infrastructure.web.contract.OpenApiContractTest}.
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponseDto> handleMessageNotReadable(HttpMessageNotReadableException ex) {
        return ResponseEntity.badRequest().body(new ErrorResponseDto("Malformed request body"));
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

    /**
     * Bean Validation on {@code @RequestParam}/{@code @PathVariable} (class-level
     * {@code @Validated}, e.g. {@code SupplierController#potentialSuppliers}'s {@code rate}/
     * {@code limit}/{@code offset}) fails with this exception via
     * {@code MethodValidationInterceptor}, not {@link MethodArgumentNotValidException} — found by
     * actually calling the endpoint with an out-of-range {@code rate}/{@code limit}/{@code offset}
     * and getting an uncaught 500 instead of the OpenAPI-documented 400.
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponseDto> handleConstraintViolation(ConstraintViolationException ex) {
        String message = ex.getConstraintViolations().stream()
                .map(violation -> violation.getPropertyPath() + " " + violation.getMessage())
                .collect(Collectors.joining(", "));
        return ResponseEntity.badRequest().body(new ErrorResponseDto(message));
    }
}
