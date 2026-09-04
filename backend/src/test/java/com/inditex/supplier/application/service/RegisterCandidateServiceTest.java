package com.inditex.supplier.application.service;

import com.inditex.supplier.application.port.out.SupplierRepositoryPort;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * TODO coverage (mock {@link SupplierRepositoryPort}):
 * <ul>
 *   <li>No existing record for DUNS → saves a new {@code CANDIDATE} record.</li>
 *   <li>Existing record in {@code BANNED} → {@code SupplierBannedException}, no save.</li>
 *   <li>Existing record in any other status (including {@code REFUSED}, per no-reapply decision)
 *       → {@code CandidateAlreadyExistsException}, no save.</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
class RegisterCandidateServiceTest {

    @Mock
    private SupplierRepositoryPort supplierRepositoryPort;

    @Test
    @Disabled("TODO: implement - see class javadoc")
    void registersNewCandidateWhenNoExistingRecord() {
    }

    @Test
    @Disabled("TODO: implement - see class javadoc")
    void throwsSupplierBannedWhenExistingRecordIsBanned() {
    }

    @Test
    @Disabled("TODO: implement - see class javadoc")
    void throwsCandidateAlreadyExistsForAnyOtherExistingStatus() {
    }
}
