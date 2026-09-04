package com.inditex.supplier.infrastructure.web.controller;

import com.inditex.supplier.application.port.in.AcceptCandidateUseCase;
import com.inditex.supplier.application.port.in.GetCandidateUseCase;
import com.inditex.supplier.application.port.in.RefuseCandidateUseCase;
import com.inditex.supplier.application.port.in.RegisterCandidateUseCase;
import com.inditex.supplier.domain.exception.CandidateAlreadyExistsException;
import com.inditex.supplier.domain.exception.CandidateNotRefusableException;
import com.inditex.supplier.domain.model.AnnualTurnover;
import com.inditex.supplier.domain.model.CountryCode;
import com.inditex.supplier.domain.model.Duns;
import com.inditex.supplier.domain.model.SupplierRecord;
import com.inditex.supplier.infrastructure.web.mapper.CandidateWebMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * {@code @WebMvcTest(CandidateController.class)} with mocked use cases via {@code @MockBean},
 * asserting HTTP status codes and JSON shape match the OpenAPI contract. {@link CandidateWebMapper}
 * is imported for real (a plain deterministic mapper, not a use case) rather than mocked.
 */
@WebMvcTest(CandidateController.class)
@Import(CandidateWebMapper.class)
class CandidateControllerTest {

    private static final Duns DUNS = new Duns(123_456_789);

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

    @Test
    void addCandidateReturns201OnSuccess() throws Exception {
        SupplierRecord record = SupplierRecord.apply(DUNS, "Zippers & Buttons", new CountryCode("ES"),
                new AnnualTurnover(2_000_000L));
        given(registerCandidateUseCase.register(any())).willReturn(record);

        mockMvc.perform(post("/candidates")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"duns":123456789,"name":"Zippers & Buttons","country":"ES","annualTurnover":2000000}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.duns").value(123456789))
                .andExpect(jsonPath("$.name").value("Zippers & Buttons"))
                .andExpect(jsonPath("$.status").doesNotExist());
    }

    @Test
    void addCandidateReturns400OnInvalidBody() throws Exception {
        mockMvc.perform(post("/candidates")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"country":"ES","annualTurnover":2000000}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void addCandidateReturns409OnAlreadyExists() throws Exception {
        given(registerCandidateUseCase.register(any())).willThrow(new CandidateAlreadyExistsException(DUNS));

        mockMvc.perform(post("/candidates")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"duns":123456789,"name":"Zippers & Buttons","country":"ES","annualTurnover":2000000}
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.info").value("Candidate already exists"));
    }

    @Test
    void getCandidateReturns404WhenNotVisible() throws Exception {
        given(getCandidateUseCase.getByDuns(DUNS.value())).willReturn(Optional.empty());

        mockMvc.perform(get("/candidates/{duns}", DUNS.value()))
                .andExpect(status().isNotFound());
    }

    @Test
    void acceptCandidateReturns204OnSuccess() throws Exception {
        mockMvc.perform(post("/candidates/{duns}/accept", DUNS.value())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"sustainabilityRating":"A"}
                                """))
                .andExpect(status().isNoContent());
    }

    @Test
    void refuseCandidateReturns409WhenNotRefusable() throws Exception {
        doThrow(new CandidateNotRefusableException(DUNS)).when(refuseCandidateUseCase).refuse(DUNS.value());

        mockMvc.perform(post("/candidates/{duns}/refuse", DUNS.value()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.info").value("Candidate can not be refused"));
    }
}
