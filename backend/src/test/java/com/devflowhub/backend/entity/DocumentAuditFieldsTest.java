package com.devflowhub.backend.entity;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class DocumentAuditFieldsTest {

    @Test
    void onCreateInitializesBothAuditFields() {
        Document document = new Document();

        document.onCreate();

        assertThat(document.getCreatedAt()).isNotNull();
        assertThat(document.getUpdatedAt())
                .isEqualTo(document.getCreatedAt());
    }

    @Test
    void onCreateReplacesClientSuppliedAuditValues() {
        Document document = new Document();

        LocalDateTime suppliedCreatedAt =
                LocalDateTime.of(2000, 1, 1, 0, 0);

        LocalDateTime suppliedUpdatedAt =
                LocalDateTime.of(2000, 1, 2, 0, 0);

        document.setCreatedAt(suppliedCreatedAt);
        document.setUpdatedAt(suppliedUpdatedAt);

        document.onCreate();

        assertThat(document.getCreatedAt())
                .isAfter(suppliedCreatedAt);

        assertThat(document.getUpdatedAt())
                .isEqualTo(document.getCreatedAt());
    }

    @Test
    void onUpdatePreservesCreatedAtAndRefreshesUpdatedAt() {
        Document document = new Document();
        document.onCreate();

        LocalDateTime createdAt = document.getCreatedAt();
        LocalDateTime oldUpdatedAt =
                LocalDateTime.of(2000, 1, 1, 0, 0);

        document.setUpdatedAt(oldUpdatedAt);

        document.onUpdate();

        assertThat(document.getCreatedAt())
                .isEqualTo(createdAt);

        assertThat(document.getUpdatedAt())
                .isAfter(oldUpdatedAt);
    }
}