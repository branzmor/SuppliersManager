package com.supplier.management.domain.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * Valid 2-letter uppercase codes accepted; lowercase, wrong length, or non-alpha input rejected.
 * Design decision (see {@link CountryCode} javadoc): strict validation, no lower-to-upper
 * normalization.
 */
class CountryCodeTest {

    @Test
    void acceptsValidIsoCode() {
        assertThat(new CountryCode("ES").isoCode()).isEqualTo("ES");
    }

    @Test
    void rejectsInvalidIsoCode() {
        assertThatIllegalArgumentException().isThrownBy(() -> new CountryCode("es"));
        assertThatIllegalArgumentException().isThrownBy(() -> new CountryCode("ESP"));
        assertThatIllegalArgumentException().isThrownBy(() -> new CountryCode("E1"));
        assertThatIllegalArgumentException().isThrownBy(() -> new CountryCode(null));
    }
}
