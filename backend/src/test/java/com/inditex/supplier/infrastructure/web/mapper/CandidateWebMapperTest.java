package com.inditex.supplier.infrastructure.web.mapper;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

/**
 * TODO coverage: request DTO → command carries all 4 fields verbatim; domain record → response
 * DTO carries all 4 {@code Candidate} schema fields, and — important — does NOT include
 * {@code status} (the {@code Candidate} schema has no status field, only {@code Supplier} does).
 */
class CandidateWebMapperTest {

    @Test
    @Disabled("TODO: implement - see class javadoc")
    void mapsRequestDtoToCommand() {
    }

    @Test
    @Disabled("TODO: implement - see class javadoc")
    void mapsDomainRecordToResponseDto() {
    }
}
