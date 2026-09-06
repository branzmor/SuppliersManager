package com.inditex.supplier.infrastructure.web.contract;

import com.atlassian.oai.validator.OpenApiInteractionValidator;
import com.atlassian.oai.validator.mockmvc.MockMvcResponse;
import com.atlassian.oai.validator.model.Request;
import com.atlassian.oai.validator.report.LevelResolver;
import com.atlassian.oai.validator.report.ValidationReport;
import com.inditex.supplier.application.port.in.AcceptCandidateUseCase;
import com.inditex.supplier.application.port.in.BanSupplierUseCase;
import com.inditex.supplier.application.port.in.GetCandidateUseCase;
import com.inditex.supplier.application.port.in.GetPotentialSuppliersUseCase;
import com.inditex.supplier.application.port.in.GetSupplierUseCase;
import com.inditex.supplier.application.port.in.RefuseCandidateUseCase;
import com.inditex.supplier.application.port.in.RegisterCandidateUseCase;
import com.inditex.supplier.application.port.out.ScoredSupplier;
import com.inditex.supplier.domain.exception.CandidateAlreadyExistsException;
import com.inditex.supplier.domain.exception.CandidateNotAcceptableException;
import com.inditex.supplier.domain.exception.CandidateNotRefusableException;
import com.inditex.supplier.domain.exception.SupplierBannedException;
import com.inditex.supplier.domain.exception.SupplierNotBannableException;
import com.inditex.supplier.domain.exception.SupplierRecordNotFoundException;
import com.inditex.supplier.domain.model.AnnualTurnover;
import com.inditex.supplier.domain.model.CountryCode;
import com.inditex.supplier.domain.model.Duns;
import com.inditex.supplier.domain.model.SupplierRecord;
import com.inditex.supplier.domain.model.SupplierStatus;
import com.inditex.supplier.domain.model.SustainabilityRating;
import com.inditex.supplier.infrastructure.web.controller.CandidateController;
import com.inditex.supplier.infrastructure.web.controller.SupplierController;
import com.inditex.supplier.infrastructure.web.mapper.CandidateWebMapper;
import com.inditex.supplier.infrastructure.web.mapper.SupplierWebMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultMatcher;

import java.util.List;
import java.util.Optional;

import static com.atlassian.oai.validator.mockmvc.OpenApiValidationMatchers.openApi;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Contract testing: every {@code MockMvc} interaction below is asserted, in addition to its HTTP
 * status, against the real API contract at
 * {@code wiki/itx-iop_tech-supplier_flow-main-openapi3_1.yaml} (paths, parameters, request/response
 * bodies, content types and status codes) via
 * {@code com.atlassian.oai:swagger-request-validator-mockmvc}.
 *
 * <h2>Library choice and its OpenAPI 3.1 limitation (read before adding new cases)</h2>
 *
 * <p>{@code swagger-request-validator} (now {@code openapi-request-validator}) is the most mature
 * Java library that ships a ready-made MockMvc {@code ResultMatcher} for Spring Boot 3 — that
 * MockMvc integration is exactly what this class uses. Its <strong>current</strong> major version
 * (3.0.0) targets Spring Framework 7 / Spring Boot 4 and does not run on this project's Spring Boot
 * 3.3.4, so this project intentionally pins the last Spring-Boot-3-compatible release,
 * {@code 2.46.1} (see {@code pom.xml}).
 *
 * <p>That version's underlying parser (swagger-parser-v3) predates full OpenAPI 3.1 support: it
 * reads a document declaring {@code openapi: 3.1.0} without rejecting it, but validates it using
 * OpenAPI 3.0 / JSON Schema Draft-4 semantics, not the JSON Schema 2020-12 dialect 3.1 documents
 * are technically based on. Concretely, for <em>this</em> contract file:
 * <ul>
 *   <li><strong>Fully validated</strong>: paths, HTTP methods, path/query parameter presence,
 *       type and {@code minimum}/{@code maximum} bounds, required-body enforcement, response
 *       status codes, response {@code content-type}, and JSON schema shape (required properties,
 *       types, string {@code minLength}/{@code maxLength}, numeric bounds, enums) for every request
 *       and response body in this contract — none of the schemas here use a 3.1-only keyword
 *       (no {@code type} arrays for nullability, no {@code prefixItems}, no {@code const}, no
 *       boolean-form {@code exclusiveMinimum}/{@code exclusiveMaximum}).
 *   <li><strong>Not validated / silently ignored</strong>: the 3.1-style plural {@code examples:
 *       [...]} annotation keyword used throughout this spec (3.0 uses a singular {@code example}).
 *       This is a non-normative annotation, not a constraint, so a parser that does not understand
 *       it simply does not enforce anything from it — it does not cause false negatives on real
 *       constraints, it just means "examples" are not cross-checked against their own schema.
 * </ul>
 * In short: for a contract like this one — 3.1-labelled but not exercising 3.1-only schema
 * features — {@code swagger-request-validator} 2.46.1 gives fully faithful structural/type/range
 * validation. It would <strong>not</strong> be reliable against a spec that actually used
 * {@code type: [string, "null"]}, {@code prefixItems}, or numeric-form
 * {@code exclusiveMinimum}/{@code exclusiveMaximum} — those would need a JSON-Schema-2020-12-native
 * validator instead (e.g. {@code networknt/json-schema-validator}) at the cost of losing the
 * ready-made MockMvc/path-routing integration used here.
 *
 * <h2>Known, deliberate gap: HTTP 422 on {@code POST /candidates}</h2>
 *
 * <p>The contract declares a {@code 422 Unprocessable Content} response on {@code POST /candidates}
 * (see {@code #/components/responses/UnprocessableContent}). No test in this class exercises it,
 * and none of the application's exception handling ever returns it — see
 * {@code GlobalExceptionHandler}'s class javadoc and {@code SOLUTION.md} §"Gap conocido: 422 en
 * POST /candidates" for why: no rule in the README distinguishes a 422 case from the already-
 * implemented 400 (invalid schema) and 409 (duplicate/banned) responses. Contract validation
 * confirms the 422 response *shape* is declared correctly in the spec; it cannot and does not
 * fabricate a business scenario that would trigger it.
 *
 * <h2>Known, real discrepancy: invalid path parameters return an undeclared 400</h2>
 *
 * <p><strong>This is a genuine gap in contract compliance, not a tooling limitation</strong> — call
 * it out explicitly rather than let the "fully validated" claim above imply 36/36 green means the
 * implementation is 100% contract-compliant. {@code GET /candidates/{duns}}, {@code GET
 * /suppliers/{duns}}, {@code POST /candidates/{duns}/refuse} and {@code POST /suppliers/{duns}/ban}
 * each declare only 2xx/404/409 responses — never 400 — for an invalid {@code duns}. In practice, a
 * syntactically-valid but out-of-range {@code duns} (outside {@code [100000000, 999999999]}) makes
 * {@code new Duns(duns)} throw {@code IllegalArgumentException} before the "not found" branch can
 * run, which {@code GlobalExceptionHandler} maps to a 400 the contract never declares for that
 * operation. See {@code getCandidateReturns400ForADunsOutsideTheContractRangeUndeclaredButReal}
 * below, which asserts the status code only — deliberately without {@link #MATCHES_CONTRACT} or
 * {@link #matchesResponseContractOnly()}, since neither would (or should) pass for a response the
 * contract doesn't declare. Not fixed here: see {@code SOLUTION.md}
 * §"Validación automática contra el contrato OpenAPI" for why this is out of scope for "add
 * contract testing".
 */
@WebMvcTest({CandidateController.class, SupplierController.class})
@Import({CandidateWebMapper.class, SupplierWebMapper.class})
class OpenApiContractTest {

    private static final String SPEC_PATH = "../wiki/itx-iop_tech-supplier_flow-main-openapi3_1.yaml";
    // The spec composes Supplier/PotentialSupplier via `allOf: [Candidate, {extra fields}]` with
    // no `additionalProperties` keyword anywhere - per plain JSON Schema semantics that means each
    // allOf branch simply doesn't care about properties outside its own list, so validating them
    // independently should pass once every property is covered by *some* branch. This validator
    // instead defaults to injecting `additionalProperties: false` into every object schema unless
    // told not to (a documented, deliberate "gotcha" - see its own SchemaValidator source, method
    // checkForKnownGotchasAndLogMessage) - which turns allOf composition itself into a false
    // failure, since no single branch declares 100% of the final object's properties. Disabling it
    // via this exact message key is the library's own documented escape hatch for this, not a
    // general "ignore schema errors" switch - every other schema-validation message key (missing
    // required property, wrong type, out-of-range, wrong enum value, wrong content-type, ...)
    // still fails the test normally.
    private static final LevelResolver LEVEL_RESOLVER = LevelResolver.create()
            .withLevel("validation.schema.additionalProperties", ValidationReport.Level.IGNORE)
            .build();
    private static final OpenApiInteractionValidator VALIDATOR =
            OpenApiInteractionValidator.createFor(SPEC_PATH)
                    .withLevelResolver(LEVEL_RESOLVER)
                    .build();
    private static final ResultMatcher MATCHES_CONTRACT = openApi().isValid(VALIDATOR);

    private static final int MIN_DUNS = 100_000_000;
    private static final int MAX_DUNS = 999_999_999;

    /**
     * Validates only the HTTP response against the contract, not the request.
     *
     * <p>{@link #MATCHES_CONTRACT} validates the full recorded interaction — request included —
     * which is right for a well-formed request. It is the wrong tool for a test whose entire
     * point is that the <em>request itself</em> deliberately violates the contract (a missing
     * required field, a {@code duns}/{@code rate}/{@code limit}/{@code offset} outside its declared
     * bounds): such a request can never "match the contract", by construction. What these tests
     * actually need to confirm is that the *response* — the declared 400 and its {@code Error}
     * schema — is what the contract promises for that operation, regardless of why the request was
     * rejected.
     */
    private static ResultMatcher matchesResponseContractOnly() {
        return result -> {
            ValidationReport report = VALIDATOR.validateResponse(
                    result.getRequest().getRequestURI(),
                    Request.Method.valueOf(result.getRequest().getMethod()),
                    MockMvcResponse.of(result.getResponse()));
            if (report.hasErrors()) {
                throw new AssertionError("OpenAPI response validation failed:\n" + report.getMessages());
            }
        };
    }

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private RegisterCandidateUseCase registerCandidateUseCase;
    @MockBean
    private GetCandidateUseCase getCandidateUseCase;
    @MockBean
    private AcceptCandidateUseCase acceptCandidateUseCase;
    @MockBean
    private RefuseCandidateUseCase refuseCandidateUseCase;
    @MockBean
    private GetPotentialSuppliersUseCase getPotentialSuppliersUseCase;
    @MockBean
    private GetSupplierUseCase getSupplierUseCase;
    @MockBean
    private BanSupplierUseCase banSupplierUseCase;

    private static SupplierRecord candidate(int duns) {
        return SupplierRecord.apply(new Duns(duns), "Zippers & Buttons", new CountryCode("ES"),
                new AnnualTurnover(2_000_000L));
    }

    private static String candidateBody(int duns) {
        return """
                {"duns":%d,"name":"Zippers & Buttons","country":"ES","annualTurnover":2000000}
                """.formatted(duns);
    }

    // ---------------------------------------------------------------------------------------
    // POST /candidates -> 201, 400, 409 (both variants)
    // ---------------------------------------------------------------------------------------

    @Test
    void addCandidateReturns201AndMatchesContract() throws Exception {
        given(registerCandidateUseCase.register(any())).willReturn(candidate(123_456_789));

        mockMvc.perform(post("/candidates")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(candidateBody(123_456_789)))
                .andExpect(status().isCreated())
                .andExpect(MATCHES_CONTRACT);
    }

    @ParameterizedTest
    @ValueSource(ints = {MIN_DUNS, MAX_DUNS})
    void addCandidateAcceptsTheDunsBoundaryValuesAndMatchesContract(int duns) throws Exception {
        given(registerCandidateUseCase.register(any())).willReturn(candidate(duns));

        mockMvc.perform(post("/candidates")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(candidateBody(duns)))
                .andExpect(status().isCreated())
                .andExpect(MATCHES_CONTRACT);
    }

    @Test
    void addCandidateReturns400WhenARequiredFieldIsMissingAndMatchesContract() throws Exception {
        // The request itself is deliberately non-conforming (that's the point of the test), so
        // only the response - the declared 400 and its Error schema - is checked against the
        // contract; see matchesResponseContractOnly()'s javadoc.
        mockMvc.perform(post("/candidates")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"country":"ES","annualTurnover":2000000}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(matchesResponseContractOnly());
    }

    @Test
    void addCandidateReturns400WhenDunsIsOutOfRangeAndMatchesContract() throws Exception {
        mockMvc.perform(post("/candidates")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(candidateBody(MAX_DUNS + 1)))
                .andExpect(status().isBadRequest())
                .andExpect(matchesResponseContractOnly());
    }

    @Test
    void addCandidateReturns409OnAlreadyExistsAndMatchesContract() throws Exception {
        given(registerCandidateUseCase.register(any()))
                .willThrow(new CandidateAlreadyExistsException(new Duns(123_456_789)));

        mockMvc.perform(post("/candidates")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(candidateBody(123_456_789)))
                .andExpect(status().isConflict())
                .andExpect(MATCHES_CONTRACT);
    }

    @Test
    void addCandidateReturns409OnSupplierBannedAndMatchesContract() throws Exception {
        given(registerCandidateUseCase.register(any()))
                .willThrow(new SupplierBannedException(new Duns(123_456_789)));

        mockMvc.perform(post("/candidates")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(candidateBody(123_456_789)))
                .andExpect(status().isConflict())
                .andExpect(MATCHES_CONTRACT);
    }

    // ---------------------------------------------------------------------------------------
    // GET /candidates/{duns} -> 200, 404
    // ---------------------------------------------------------------------------------------

    @ParameterizedTest
    @ValueSource(ints = {MIN_DUNS, MAX_DUNS})
    void getCandidateReturns200AtTheDunsBoundariesAndMatchesContract(int duns) throws Exception {
        given(getCandidateUseCase.getByDuns(duns)).willReturn(Optional.of(candidate(duns)));

        mockMvc.perform(get("/candidates/{duns}", duns))
                .andExpect(status().isOk())
                .andExpect(MATCHES_CONTRACT);
    }

    @Test
    void getCandidateReturns404WhenNotFoundAndMatchesContract() throws Exception {
        given(getCandidateUseCase.getByDuns(anyInt())).willReturn(Optional.empty());

        mockMvc.perform(get("/candidates/{duns}", 123_456_789))
                .andExpect(status().isNotFound())
                .andExpect(MATCHES_CONTRACT);
    }

    @Test
    void getCandidateReturns400ForADunsOutsideTheContractRangeUndeclaredButReal() throws Exception {
        // A genuine finding surfaced by this contract suite, documented rather than silently
        // fixed (out of scope for "add contract testing"): GetCandidateController#getCandidate
        // builds `new Duns(duns)` on the not-found path (for the exception's message), and Duns's
        // own constructor validates the [100000000, 999999999] range - so a syntactically-valid
        // but out-of-contract-range path duns throws IllegalArgumentException, mapped by
        // GlobalExceptionHandler to 400, *before* the mocked "not found" 404 can ever be returned.
        // The contract only declares 200/404 for this operation, not 400 - so this specific
        // interaction does not (and cannot) match the contract, and is deliberately not asserted
        // with matchesResponseContractOnly()/MATCHES_CONTRACT. Not fixed here: doing so would mean
        // changing GetCandidateService's/the controller's existing behaviour, which is outside the
        // "add OpenAPI contract testing" gap this class exists for. The same DUNS-range-vs-404
        // ordering applies uniformly to every {duns} path endpoint (see SOLUTION.md).
        given(getCandidateUseCase.getByDuns(anyInt())).willReturn(Optional.empty());

        mockMvc.perform(get("/candidates/{duns}", MAX_DUNS + 1))
                .andExpect(status().isBadRequest());
    }

    // ---------------------------------------------------------------------------------------
    // POST /candidates/{duns}/accept -> 204, 400, 404, 409
    // ---------------------------------------------------------------------------------------

    @Test
    void acceptCandidateReturns204AndMatchesContract() throws Exception {
        mockMvc.perform(post("/candidates/{duns}/accept", 123_456_789)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"sustainabilityRating":"A"}
                                """))
                .andExpect(status().isNoContent())
                .andExpect(MATCHES_CONTRACT);
    }

    @Test
    void acceptCandidateReturns400WhenRatingIsMissingAndMatchesContract() throws Exception {
        mockMvc.perform(post("/candidates/{duns}/accept", 123_456_789)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(matchesResponseContractOnly());
    }

    @Test
    void acceptCandidateReturns400WhenRatingIsNotAValidEnumValueAndMatchesContract() throws Exception {
        mockMvc.perform(post("/candidates/{duns}/accept", 123_456_789)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"sustainabilityRating":"Z"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(matchesResponseContractOnly());
    }

    @Test
    void acceptCandidateReturns404WhenNotFoundAndMatchesContract() throws Exception {
        doThrow(new SupplierRecordNotFoundException(new Duns(123_456_789)))
                .when(acceptCandidateUseCase).accept(eq(123_456_789), any());

        mockMvc.perform(post("/candidates/{duns}/accept", 123_456_789)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"sustainabilityRating":"A"}
                                """))
                .andExpect(status().isNotFound())
                .andExpect(MATCHES_CONTRACT);
    }

    @Test
    void acceptCandidateReturns409WhenNotAcceptableAndMatchesContract() throws Exception {
        doThrow(new CandidateNotAcceptableException(new Duns(123_456_789), "country not approved"))
                .when(acceptCandidateUseCase).accept(eq(123_456_789), any());

        mockMvc.perform(post("/candidates/{duns}/accept", 123_456_789)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"sustainabilityRating":"A"}
                                """))
                .andExpect(status().isConflict())
                .andExpect(MATCHES_CONTRACT);
    }

    // ---------------------------------------------------------------------------------------
    // POST /candidates/{duns}/refuse -> 204, 404, 409
    // ---------------------------------------------------------------------------------------

    @Test
    void refuseCandidateReturns204AndMatchesContract() throws Exception {
        mockMvc.perform(post("/candidates/{duns}/refuse", 123_456_789))
                .andExpect(status().isNoContent())
                .andExpect(MATCHES_CONTRACT);
    }

    @Test
    void refuseCandidateReturns404WhenNotFoundAndMatchesContract() throws Exception {
        doThrow(new SupplierRecordNotFoundException(new Duns(123_456_789)))
                .when(refuseCandidateUseCase).refuse(123_456_789);

        mockMvc.perform(post("/candidates/{duns}/refuse", 123_456_789))
                .andExpect(status().isNotFound())
                .andExpect(MATCHES_CONTRACT);
    }

    @Test
    void refuseCandidateReturns409WhenNotRefusableAndMatchesContract() throws Exception {
        doThrow(new CandidateNotRefusableException(new Duns(123_456_789)))
                .when(refuseCandidateUseCase).refuse(123_456_789);

        mockMvc.perform(post("/candidates/{duns}/refuse", 123_456_789))
                .andExpect(status().isConflict())
                .andExpect(MATCHES_CONTRACT);
    }

    // ---------------------------------------------------------------------------------------
    // GET /suppliers/{duns} -> 200, 404
    // ---------------------------------------------------------------------------------------

    @ParameterizedTest
    @ValueSource(ints = {MIN_DUNS, MAX_DUNS})
    void getSupplierReturns200AtTheDunsBoundariesAndMatchesContract(int duns) throws Exception {
        SupplierRecord active = SupplierRecord.reconstitute(new Duns(duns), "Zippers & Buttons",
                new CountryCode("ES"), new AnnualTurnover(2_000_000L), SupplierStatus.ACTIVE, SustainabilityRating.A);
        given(getSupplierUseCase.getByDuns(duns)).willReturn(Optional.of(active));

        mockMvc.perform(get("/suppliers/{duns}", duns))
                .andExpect(status().isOk())
                .andExpect(MATCHES_CONTRACT);
    }

    @Test
    void getSupplierReturns404WhenNotFoundAndMatchesContract() throws Exception {
        given(getSupplierUseCase.getByDuns(anyInt())).willReturn(Optional.empty());

        mockMvc.perform(get("/suppliers/{duns}", 123_456_789))
                .andExpect(status().isNotFound())
                .andExpect(MATCHES_CONTRACT);
    }

    // ---------------------------------------------------------------------------------------
    // POST /suppliers/{duns}/ban -> 204, 404, 409
    // ---------------------------------------------------------------------------------------

    @Test
    void banSupplierReturns204AndMatchesContract() throws Exception {
        mockMvc.perform(post("/suppliers/{duns}/ban", 123_456_789))
                .andExpect(status().isNoContent())
                .andExpect(MATCHES_CONTRACT);
    }

    @Test
    void banSupplierReturns404WhenNotFoundAndMatchesContract() throws Exception {
        doThrow(new SupplierRecordNotFoundException(new Duns(123_456_789)))
                .when(banSupplierUseCase).ban(123_456_789);

        mockMvc.perform(post("/suppliers/{duns}/ban", 123_456_789))
                .andExpect(status().isNotFound())
                .andExpect(MATCHES_CONTRACT);
    }

    @Test
    void banSupplierReturns409WhenNotBannableAndMatchesContract() throws Exception {
        doThrow(new SupplierNotBannableException(new Duns(123_456_789)))
                .when(banSupplierUseCase).ban(123_456_789);

        mockMvc.perform(post("/suppliers/{duns}/ban", 123_456_789))
                .andExpect(status().isConflict())
                .andExpect(MATCHES_CONTRACT);
    }

    // ---------------------------------------------------------------------------------------
    // GET /suppliers/potential -> 200, 400
    // ---------------------------------------------------------------------------------------

    @Test
    void potentialSuppliersReturns200WithAFullyPopulatedItemAndMatchesContract() throws Exception {
        SupplierRecord onProbation = SupplierRecord.reconstitute(new Duns(123_456_789), "Zippers & Buttons",
                new CountryCode("ES"), new AnnualTurnover(2_000_000L), SupplierStatus.ON_PROBATION, SustainabilityRating.C);
        ScoredSupplier scored = new ScoredSupplier(onProbation, 123_456.78);
        given(getPotentialSuppliersUseCase.getPotentialSuppliers(any()))
                .willReturn(new GetPotentialSuppliersUseCase.Result(List.of(scored), 10, 0, 1L));

        mockMvc.perform(get("/suppliers/potential").param("rate", "250"))
                .andExpect(status().isOk())
                .andExpect(MATCHES_CONTRACT);
    }

    @Test
    void potentialSuppliersReturns200WithAnEmptyResultAndMatchesContract() throws Exception {
        given(getPotentialSuppliersUseCase.getPotentialSuppliers(any()))
                .willReturn(new GetPotentialSuppliersUseCase.Result(List.of(), 10, 0, 0L));

        mockMvc.perform(get("/suppliers/potential").param("rate", "5000"))
                .andExpect(status().isOk())
                .andExpect(MATCHES_CONTRACT);
    }

    @ParameterizedTest
    @ValueSource(longs = {250L, 251L})
    void potentialSuppliersAcceptsRateAtOrAboveTheMinimumAndMatchesContract(long rate) throws Exception {
        given(getPotentialSuppliersUseCase.getPotentialSuppliers(any()))
                .willReturn(new GetPotentialSuppliersUseCase.Result(List.of(), 10, 0, 0L));

        mockMvc.perform(get("/suppliers/potential").param("rate", String.valueOf(rate)))
                .andExpect(status().isOk())
                .andExpect(MATCHES_CONTRACT);
    }

    @Test
    void potentialSuppliersReturns400WhenRateIsBelowTheMinimumAndMatchesContract() throws Exception {
        mockMvc.perform(get("/suppliers/potential").param("rate", "249"))
                .andExpect(status().isBadRequest())
                .andExpect(matchesResponseContractOnly());
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 10})
    void potentialSuppliersAcceptsLimitAtItsBoundariesAndMatchesContract(int limit) throws Exception {
        given(getPotentialSuppliersUseCase.getPotentialSuppliers(any()))
                .willReturn(new GetPotentialSuppliersUseCase.Result(List.of(), limit, 0, 0L));

        mockMvc.perform(get("/suppliers/potential").param("rate", "250").param("limit", String.valueOf(limit)))
                .andExpect(status().isOk())
                .andExpect(MATCHES_CONTRACT);
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 11})
    void potentialSuppliersReturns400WhenLimitIsOutsideItsBoundsAndMatchesContract(int limit) throws Exception {
        mockMvc.perform(get("/suppliers/potential").param("rate", "250").param("limit", String.valueOf(limit)))
                .andExpect(status().isBadRequest())
                .andExpect(matchesResponseContractOnly());
    }

    @Test
    void potentialSuppliersAcceptsOffsetZeroAndMatchesContract() throws Exception {
        given(getPotentialSuppliersUseCase.getPotentialSuppliers(any()))
                .willReturn(new GetPotentialSuppliersUseCase.Result(List.of(), 10, 0, 0L));

        mockMvc.perform(get("/suppliers/potential").param("rate", "250").param("offset", "0"))
                .andExpect(status().isOk())
                .andExpect(MATCHES_CONTRACT);
    }

    @Test
    void potentialSuppliersReturns400WhenOffsetIsNegativeAndMatchesContract() throws Exception {
        mockMvc.perform(get("/suppliers/potential").param("rate", "250").param("offset", "-1"))
                .andExpect(status().isBadRequest())
                .andExpect(matchesResponseContractOnly());
    }
}
