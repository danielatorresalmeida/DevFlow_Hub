package com.devflowhub.backend.storage;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public abstract class ObjectStorageContractTest {

    @TempDir
    protected Path temporaryDirectory;

    private ObjectStorage objectStorage;

    protected abstract ObjectStorage createStorage(
            Path rootDirectory
    );

    @BeforeEach
    protected void setUpObjectStorage() {
        objectStorage = createStorage(
                temporaryDirectory.resolve("objects")
        );
    }

    @Test
    protected void storesAndReadsObjectWithMetadata()
            throws Exception {
        byte[] expectedContent =
                "hello object storage"
                        .getBytes(StandardCharsets.UTF_8);

        String key = "documents/7/report.txt";

        ObjectStorageMetadata storedMetadata =
                objectStorage.put(
                        key,
                        new ByteArrayInputStream(
                                expectedContent
                        )
                );

        assertThat(storedMetadata.key())
                .isEqualTo(key);

        assertThat(storedMetadata.sizeBytes())
                .isEqualTo(expectedContent.length);

        assertThat(storedMetadata.sha256())
                .isEqualTo(sha256(expectedContent));

        assertThat(storedMetadata.providerVersion())
                .isNotBlank();

        try (
            ObjectStorageObject storedObject =
                    objectStorage.get(key)
        ) {
            assertThat(storedObject.metadata())
                    .isEqualTo(storedMetadata);

            assertThat(
                    storedObject.content().readAllBytes()
            ).isEqualTo(expectedContent);
        }
    }

    @Test
    protected void replacesObjectStoredUnderTheSameKey()
            throws Exception {
        String key = "documents/7/replace.txt";

        ObjectStorageMetadata firstMetadata =
                objectStorage.put(
                        key,
                        new ByteArrayInputStream(
                                "first".getBytes(
                                        StandardCharsets.UTF_8
                                )
                        )
                );

        byte[] replacement =
                "other".getBytes(
                        StandardCharsets.UTF_8
                );

        ObjectStorageMetadata replacementMetadata =
                objectStorage.put(
                        key,
                        new ByteArrayInputStream(
                                replacement
                        )
                );

        assertThat(replacementMetadata.sizeBytes())
                .isEqualTo(replacement.length);

        assertThat(replacementMetadata.sha256())
                .isEqualTo(sha256(replacement));

        assertThat(
                replacementMetadata.providerVersion()
        ).isNotEqualTo(
                firstMetadata.providerVersion()
        );

        try (
            ObjectStorageObject storedObject =
                    objectStorage.get(key)
        ) {
            assertThat(
                    storedObject.content().readAllBytes()
            ).isEqualTo(replacement);
        }
    }

    @Test
    protected void metadataExistsAndDeleteReflectLifecycle() {
        String key = "documents/8/lifecycle.bin";

        assertThat(objectStorage.exists(key))
                .isFalse();

        assertThat(
                objectStorage.findMetadata(key)
        ).isEmpty();

        ObjectStorageMetadata stored =
                objectStorage.put(
                        key,
                        new ByteArrayInputStream(
                                new byte[] {
                                    1,
                                    2,
                                    3
                                }
                        )
                );

        assertThat(objectStorage.exists(key))
                .isTrue();

        assertThat(
                objectStorage.findMetadata(key)
        )
                .isPresent()
                .contains(stored);

        objectStorage.delete(key);

        assertThat(objectStorage.exists(key))
                .isFalse();

        assertThat(
                objectStorage.findMetadata(key)
        ).isEmpty();

        assertThatCode(
                () -> objectStorage.delete(key)
        ).doesNotThrowAnyException();
    }

    @Test
    protected void supportsEmptyObjects()
            throws Exception {
        String key = "documents/9/empty.bin";

        ObjectStorageMetadata metadata =
                objectStorage.put(
                        key,
                        new ByteArrayInputStream(
                                new byte[0]
                        )
                );

        assertThat(metadata.sizeBytes())
                .isZero();

        assertThat(metadata.sha256())
                .isEqualTo(
                        sha256(new byte[0])
                );

        try (
            ObjectStorageObject storedObject =
                    objectStorage.get(key)
        ) {
            assertThat(
                    storedObject.content().readAllBytes()
            ).isEmpty();
        }
    }

    @Test
    protected void rejectsUnsafeOrInvalidKeys() {
        List<String> invalidKeys = List.of(
                "",
                " ",
                "/absolute.txt",
                "documents/",
                "documents//report.txt",
                "documents/../report.txt",
                "documents/./report.txt",
                "documents\\report.txt",
                "C:/report.txt",
                "documents/report file.txt"
        );

        for (String invalidKey : invalidKeys) {
            assertThatThrownBy(
                    () -> objectStorage.put(
                            invalidKey,
                            new ByteArrayInputStream(
                                    new byte[] {1}
                            )
                    )
            )
                    .as(
                            "key should be rejected: %s",
                            invalidKey
                    )
                    .isInstanceOf(
                            ObjectStorageException.class
                    );
        }

        assertThatThrownBy(
                () -> objectStorage.put(
                        null,
                        new ByteArrayInputStream(
                                new byte[] {1}
                        )
                )
        ).isInstanceOf(
                ObjectStorageException.class
        );

        assertThatThrownBy(
                () -> objectStorage.put(
                        "documents/null.bin",
                        null
                )
        ).isInstanceOf(
                ObjectStorageException.class
        );
    }

    @Test
    protected void getRejectsMissingObject() {
        assertThatThrownBy(
                () -> objectStorage.get(
                        "documents/99/missing.bin"
                )
        )
                .isInstanceOf(
                        ObjectStorageNotFoundException.class
                )
                .hasMessage(
                        "Object not found: " +
                        "documents/99/missing.bin"
                );
    }

    @Test
    protected void putDoesNotCloseCallerInputStream()
            throws Exception {
        CloseTrackingInputStream inputStream =
                new CloseTrackingInputStream(
                        "open"
                                .getBytes(
                                        StandardCharsets.UTF_8
                                )
                );

        objectStorage.put(
                "documents/10/open.txt",
                inputStream
        );

        assertThat(inputStream.isClosed())
                .isFalse();

        inputStream.close();

        assertThat(inputStream.isClosed())
                .isTrue();
    }

    private String sha256(byte[] content) {
        try {
            MessageDigest messageDigest =
                    MessageDigest.getInstance(
                            "SHA-256"
                    );

            return HexFormat.of().formatHex(
                    messageDigest.digest(content)
            );
        }
        catch (NoSuchAlgorithmException exception) {
            throw new AssertionError(
                    "SHA-256 is unavailable.",
                    exception
            );
        }
    }

    private static final class CloseTrackingInputStream
            extends ByteArrayInputStream {

        private boolean closed;

        private CloseTrackingInputStream(
                byte[] content
        ) {
            super(content);
        }

        @Override
        public void close() throws IOException {
            closed = true;
            super.close();
        }

        private boolean isClosed() {
            return closed;
        }
    }
}
