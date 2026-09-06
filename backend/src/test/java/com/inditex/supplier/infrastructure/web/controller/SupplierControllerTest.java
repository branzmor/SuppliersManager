package com.inditex.supplier.infrastructure.web.controller;

import com.inditex.supplier.application.port.in.BanSupplierUseCase;
import com.inditex.supplier.application.port.in.GetPotentialSuppliersUseCase;
import com.inditex.supplier.application.port.in.GetSupplierUseCase;
import com.inditex.supplier.domain.exception.SupplierNotBannableException;
import com.inditex.supplier.domain.model.Duns;
import com.inditex.supplier.infrastructure.web.mapper.SupplierWebMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * {@code @WebMvcTest(SupplierController.class)} with mocked use cases. {@link SupplierWebMapper}
 * is imported for real (a plain deterministic mapper, not a use case) rather than mocked.
 */
@WebMvcTest(SupplierController.class)
@Import(SupplierWebMapper.class)
class SupplierControllerTest {

    private static final Duns DUNS = new Duns(123_456_789);

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private GetPotentialSuppliersUseCase getPotentialSuppliersUseCase;

    @MockBean
    private GetSupplierUseCase getSupplierUseCase;

    @MockBean
    private BanSupplierUseCase banSupplierUseCase;

    @Test
    void potentialSuppliersReturns200WithPagination() throws Exception {
        given(getPotentialSuppliersUseCase.getPotentialSuppliers(any()))
                .willReturn(new GetPotentialSuppliersUseCase.Result(List.of(), 10, 0, 0L));

        mockMvc.perform(get("/suppliers/potential").param("rate", "250"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.pagination.limit").value(10))
                .andExpect(jsonPath("$.pagination.offset").value(0))
                .andExpect(jsonPath("$.pagination.total").value(0));
    }

    @Test
    void potentialSuppliersReturns400WhenRateBelowMinimum() throws Exception {
        mockMvc.perform(get("/suppliers/potential").param("rate", "249"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void potentialSuppliersReturns400WhenLimitAboveMaximum() throws Exception {
        mockMvc.perform(get("/suppliers/potential").param("rate", "250").param("limit", "11"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getSupplierReturns404WhenNotVisible() throws Exception {
        given(getSupplierUseCase.getByDuns(DUNS.value())).willReturn(Optional.empty());

        mockMvc.perform(get("/suppliers/{duns}", DUNS.value()))
                .andExpect(status().isNotFound());
    }

    @Test
    void banSupplierReturns409WhenNotOnProbation() throws Exception {
        doThrow(new SupplierNotBannableException(DUNS)).when(banSupplierUseCase).ban(DUNS.value());

        mockMvc.perform(post("/suppliers/{duns}/ban", DUNS.value()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.info").value("Supplier can not be banned"));
    }
}
