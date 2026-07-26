package com.devflowhub.backend.storage;

import com.devflowhub.backend.config.AttachmentStorageProperties;
import com.devflowhub.backend.exception.InvalidOperationException;
import com.devflowhub.backend.exception.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AttachmentFileStorageTest {

    @TempDir
    Path temporaryDirectory;

    private Path storageRoot;
    private AttachmentFileStorage storage;

    @BeforeEach
    void setUp() {
        storageRoot = temporaryDirectory.resolve("uploads");

        AttachmentStorageProperties properties =
                new AttachmentStorageProperties();

        properties.setRoot(storageRoot);

        storage = new AttachmentFileStorage(properties);
        storage.initialize();
    }

    @Test
    void initializeCreatesConfiguredRootDirectory() {
        assertThat(storageRoot)
                .exists()
                .isDirectory();
    }

    @Test
    void storeWritesFileBelowConfiguredRoot()
            throws Exception {
        String storageKey =
                "documents/7/attachment.txt";

        storage.store(
                storageKey,
                inputStream("file content")
        );

        Path storedFile = storage.load(storageKey);

        assertThat(storedFile)
                .startsWith(storageRoot)
                .exists()
                .isRegularFile();

        assertThat(Files.readString(storedFile))
                .isEqualTo("file content");

        assertThat(storage.exists(storageKey))
                .isTrue();
    }

    @Test
    void storeRejectsBlankStorageKey() {
        assertThatThrownBy(() ->
                storage.store(
                        "   ",
                        inputStream("content")
                )
        )
                .isInstanceOf(
                        InvalidOperationException.class
                )
                .hasMessage("Storage key is required.");
    }

    @Test
    void storeRejectsPathTraversal() {
        assertThatThrownBy(() ->
                storage.store(
                        "../outside.txt",
                        inputStream("content")
                )
        )
                .isInstanceOf(
                        InvalidOperationException.class
                )
                .hasMessage("The storage key is invalid.");

        assertThat(
                temporaryDirectory.resolve("outside.txt")
        ).doesNotExist();
    }

    @Test
    void storeRejectsAbsolutePath() {
        String absolutePath = temporaryDirectory
                .resolve("outside.txt")
                .toAbsolutePath()
                .toString();

        assertThatThrownBy(() ->
                storage.store(
                        absolutePath,
                        inputStream("content")
                )
        )
                .isInstanceOf(
                        InvalidOperationException.class
                )
                .hasMessage("The storage key is invalid.");
    }

    @Test
    void storeRejectsDuplicateStorageKey() {
        String storageKey =
                "documents/7/report.pdf";

        storage.store(
                storageKey,
                inputStream("first")
        );

        assertThatThrownBy(() ->
                storage.store(
                        storageKey,
                        inputStream("second")
                )
        )
                .isInstanceOf(
                        InvalidOperationException.class
                )
                .hasMessage(
                        "The storage key is already in use."
                );

        assertThat(storage.load(storageKey))
                .hasContent("first");
    }

    @Test
    void loadRejectsMissingFile() {
        assertThatThrownBy(() ->
                storage.load(
                        "documents/7/missing.pdf"
                )
        )
                .isInstanceOf(
                        ResourceNotFoundException.class
                )
                .hasMessage("Attachment file not found.");
    }

    @Test
    void deleteRemovesFileAndIsIdempotent() {
        String storageKey =
                "documents/7/delete-me.txt";

        storage.store(
                storageKey,
                inputStream("content")
        );

        storage.delete(storageKey);

        assertThat(storage.exists(storageKey))
                .isFalse();

        storage.delete(storageKey);

        assertThat(storage.exists(storageKey))
                .isFalse();
    }

    private ByteArrayInputStream inputStream(
            String content
    ) {
        return new ByteArrayInputStream(
                content.getBytes(StandardCharsets.UTF_8)
        );
    }
}