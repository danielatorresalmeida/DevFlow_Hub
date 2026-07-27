package com.devflowhub.backend.storage;

import java.util.regex.Pattern;

public record ObjectStorageMetadata(
        String key,
        long sizeBytes,
        String sha256,
        String providerVersion
) {

    private static final Pattern SHA256_PATTERN =
            Pattern.compile("^[0-9a-f]{64}$");

    public ObjectStorageMetadata {
        if (key == null || key.isBlank()) {
            throw new IllegalArgumentException(
                    "Object key is required."
            );
        }

        if (sizeBytes < 0) {
            throw new IllegalArgumentException(
                    "Object size cannot be negative."
            );
        }

        if (
            sha256 == null ||
            !SHA256_PATTERN.matcher(sha256).matches()
        ) {
            throw new IllegalArgumentException(
                    "A lowercase SHA-256 checksum is required."
            );
        }

        if (
            providerVersion == null ||
            providerVersion.isBlank()
        ) {
            throw new IllegalArgumentException(
                    "Provider version is required."
            );
        }
    }
}
