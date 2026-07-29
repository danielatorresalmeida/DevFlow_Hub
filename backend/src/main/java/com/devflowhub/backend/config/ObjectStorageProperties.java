package com.devflowhub.backend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.nio.file.Path;

@ConfigurationProperties(prefix = "storage.object")
public record ObjectStorageProperties(
        Provider provider,
        LocalProperties local
) {

    public enum Provider {
        LOCAL
    }

    public record LocalProperties(
            Path rootDirectory
    ) {
    }
}
