package com.devflowhub.backend.entity;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class AttachmentAuditFieldsTest {

    @Test
    void onCreateInitializesCreatedAt() {
        Attachment attachment = new Attachment();

        attachment.onCreate();

        assertThat(attachment.getCreatedAt()).isNotNull();
    }

    @Test
    void onCreateReplacesClientSuppliedCreatedAt() {
        Attachment attachment = new Attachment();

        LocalDateTime suppliedCreatedAt =
                LocalDateTime.of(2000, 1, 1, 0, 0);

        attachment.setCreatedAt(suppliedCreatedAt);

        attachment.onCreate();

        assertThat(attachment.getCreatedAt())
                .isAfter(suppliedCreatedAt);
    }
}