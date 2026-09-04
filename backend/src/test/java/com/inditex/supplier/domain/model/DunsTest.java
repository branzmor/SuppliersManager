package com.inditex.supplier.domain.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * Valid boundary values (100_000_000, 999_999_999) accepted; values outside the range throw
 * {@code IllegalArgumentException}.
 */
class DunsTest {

    @Test
    void acceptsBoundaryValues() {
        assertThat(new Duns(100_000_000).value()).isEqualTo(100_000_000);
        assertThat(new Duns(999_999_999).value()).isEqualTo(999_999_999);
    }

    @Test
    void rejectsOutOfRangeValues() {
        assertThatIllegalArgumentException().isThrownBy(() -> new Duns(99_999_999));
        assertThatIllegalArgumentException().isThrownBy(() -> new Duns(1_000_000_000));
    }
}
