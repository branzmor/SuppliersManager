package com.inditex.supplier.infrastructure.web.controller;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

/**
 * TODO coverage — {@code @WebMvcTest(CandidateController.class)} with mocked use cases via
 * {@code @MockBean}, asserting HTTP status codes and JSON shape match the OpenAPI contract:
 * <ul>
 *   <li>{@code POST /candidates} valid body → 201 with the Candidate JSON.</li>
 *   <li>{@code POST /candidates} invalid body (e.g. missing {@code duns}) → 400.</li>
 *   <li>{@code POST /candidates} use case throws {@code CandidateAlreadyExistsException} → 409
 *       {@code {"info":"Candidate already exists"}}.</li>
 *   <li>{@code POST /candidates} use case throws {@code SupplierBannedException} → 409
 *       {@code {"info":"Supplier banned"}}.</li>
 *   <li>{@code GET /candidates/{duns}} found → 200; not visible/not found → 404.</li>
 *   <li>{@code POST /candidates/{duns}/accept} valid rating → 204; missing rating → 400;
 *       use case throws {@code CandidateNotAcceptableException} → 409.</li>
 *   <li>{@code POST /candidates/{duns}/refuse} → 204 / 404 / 409, mirroring the above.</li>
 * </ul>
 */
class CandidateControllerTest {

    @Test
    @Disabled("TODO: implement - see class javadoc")
    void addCandidateReturns201OnSuccess() {
    }

    @Test
    @Disabled("TODO: implement - see class javadoc")
    void addCandidateReturns400OnInvalidBody() {
    }

    @Test
    @Disabled("TODO: implement - see class javadoc")
    void addCandidateReturns409OnAlreadyExists() {
    }

    @Test
    @Disabled("TODO: implement - see class javadoc")
    void getCandidateReturns404WhenNotVisible() {
    }

    @Test
    @Disabled("TODO: implement - see class javadoc")
    void acceptCandidateReturns204OnSuccess() {
    }

    @Test
    @Disabled("TODO: implement - see class javadoc")
    void refuseCandidateReturns409WhenNotRefusable() {
    }
}
