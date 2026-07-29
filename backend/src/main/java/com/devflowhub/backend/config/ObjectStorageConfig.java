package com.devflowhub.backend.config;

import com.devflowhub.backend.storage.ObjectStorage;
import com.devflowhub.backend.storage.local.LocalObjectStorage;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(ObjectStorageProperties.class)
public class ObjectStorageConfig {

    @Bean
    public ObjectStorage objectStorage(
            ObjectStorageProperties properties
    ) {
        ObjectStorageProperties.Provider provider =
                properties.provider();

        if (provider == null) {
            throw new IllegalStateException(
                    "Object storage provider must be configured."
            );
        }

        return switch (provider) {
            case LOCAL -> createLocalStorage(
                    properties.local()
            );
        };
    }

    private ObjectStorage createLocalStorage(
            ObjectStorageProperties.LocalProperties properties
    ) {
        if (
            properties == null ||
            properties.rootDirectory() == null
        ) {
            throw new IllegalStateException(
                    "Local object storage root directory " +
                    "must be configured."
            );
        }

        return new LocalObjectStorage(
                properties.rootDirectory()
        );
    }
}
