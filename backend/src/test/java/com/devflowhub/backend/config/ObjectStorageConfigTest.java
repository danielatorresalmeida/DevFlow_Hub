package com.devflowhub.backend.config;

import com.devflowhub.backend.storage.ObjectStorage;
import com.devflowhub.backend.storage.local.LocalObjectStorage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class ObjectStorageConfigTest {

    private final ApplicationContextRunner contextRunner =
            new ApplicationContextRunner()
                    .withUserConfiguration(
                            ObjectStorageConfig.class
                    );

    @Test
    void createsLocalObjectStorageBean(
            @TempDir Path temporaryDirectory
    ) {
        Path storageRoot =
                temporaryDirectory.resolve("objects");

        contextRunner
                .withPropertyValues(
                        "storage.object.provider=local",
                        "storage.object.local.root-directory=" +
                        storageRoot
                )
                .run(context -> {
                    assertThat(
                            context.getStartupFailure()
                    ).isNull();

                    assertThat(
                            context.getBeansOfType(
                                    ObjectStorage.class
                            )
                    ).hasSize(1);

                    assertThat(
                            context.getBean(ObjectStorage.class)
                    ).isInstanceOf(LocalObjectStorage.class);

                    assertThat(
                            Files.isDirectory(storageRoot)
                    ).isTrue();
                });
    }

    @Test
    void rejectsMissingProvider(
            @TempDir Path temporaryDirectory
    ) {
        contextRunner
                .withPropertyValues(
                        "storage.object.local.root-directory=" +
                        temporaryDirectory
                )
                .run(context ->
                        assertThat(
                                context.getStartupFailure()
                        )
                                .isNotNull()
                                .hasRootCauseInstanceOf(
                                        IllegalStateException.class
                                )
                                .hasRootCauseMessage(
                                        "Object storage provider " +
                                        "must be configured."
                                )
                );
    }

    @Test
    void rejectsMissingLocalRootDirectory() {
        contextRunner
                .withPropertyValues(
                        "storage.object.provider=local"
                )
                .run(context ->
                        assertThat(
                                context.getStartupFailure()
                        )
                                .isNotNull()
                                .hasRootCauseInstanceOf(
                                        IllegalStateException.class
                                )
                                .hasRootCauseMessage(
                                        "Local object storage root " +
                                        "directory must be configured."
                                )
                );
    }
}
