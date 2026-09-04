package com.inditex.supplier.domain.model;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

/**
 * TODO coverage: valid 2-letter uppercase codes accepted; lowercase, wrong length, or non-alpha
 * input rejected with {@code IllegalArgumentException}. Decide (and test) whether normalization
 * (e.g. lower->upper) is acceptable or strictly rejected — document the choice in the constructor
 * javadoc when implementing.
 */
class CountryCodeTest {

    @Test
    @Disabled("TODO: implement - see class javadoc")
    void acceptsValidIsoCode() {
    }

    @Test
    @Disabled("TODO: implement - see class javadoc")
    void rejectsInvalidIsoCode() {
    }
}
