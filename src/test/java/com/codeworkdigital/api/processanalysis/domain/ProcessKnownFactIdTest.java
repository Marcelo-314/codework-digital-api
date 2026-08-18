package com.codeworkdigital.api.processanalysis.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class ProcessKnownFactIdTest {

    @Test
    void acceptsValidValue() {
        ProcessKnownFactId id = new ProcessKnownFactId("fact-1");

        assertThat(id.value()).isEqualTo("fact-1");
    }

    @Test
    void rejectsNullValue() {
        assertThatThrownBy(() -> new ProcessKnownFactId(null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void rejectsBlankValue() {
        assertThatThrownBy(() -> new ProcessKnownFactId(" "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("value must not be blank");
    }

    @Test
    void usesValueEquality() {
        assertThat(new ProcessKnownFactId("fact-1")).isEqualTo(new ProcessKnownFactId("fact-1"));
        assertThat(new ProcessKnownFactId("fact-1")).isNotEqualTo(new ProcessKnownFactId("fact-2"));
    }
}
