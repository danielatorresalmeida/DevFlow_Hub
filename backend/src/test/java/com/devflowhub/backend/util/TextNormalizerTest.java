package com.devflowhub.backend.util;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TextNormalizerTest {

    @Test
    void normalizesTextValues() {
        assertThat(TextNormalizer.trim("  DevFlow  ")).isEqualTo("DevFlow");
        assertThat(TextNormalizer.trimToNull("   ")).isNull();
        assertThat(TextNormalizer.lower("  USER@EMAIL.COM ")).isEqualTo("user@email.com");
        assertThat(TextNormalizer.upperOrDefault(" review ", "PENDING")).isEqualTo("REVIEW");
        assertThat(TextNormalizer.upperOrDefault(" ", "PENDING")).isEqualTo("PENDING");
    }
}
