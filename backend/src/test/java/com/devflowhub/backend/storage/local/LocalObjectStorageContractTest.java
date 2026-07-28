package com.devflowhub.backend.storage.local;

import com.devflowhub.backend.storage.ObjectStorage;
import com.devflowhub.backend.storage.ObjectStorageContractTest;
import com.devflowhub.backend.storage.ObjectStorageException;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LocalObjectStorageContractTest
        extends ObjectStorageContractTest {

    @Override
    protected ObjectStorage createStorage(
            Path rootDirectory
    ) {
        return new LocalObjectStorage(rootDirectory);
    }

    @Test
    void storesContentBelowConfiguredRoot()
            throws Exception {
        Path rootDirectory =
                temporaryDirectory.resolve(
                        "explicit-local-root"
                );

        LocalObjectStorage storage =
                new LocalObjectStorage(
                        rootDirectory
                );

        byte[] content =
                "local content"
                        .getBytes(StandardCharsets.UTF_8);

        storage.put(
                "documents/11/local.txt",
                new ByteArrayInputStream(content)
        );

        Path storedFile =
                rootDirectory.resolve(
                        "documents/11/local.txt"
                );

        assertThat(
                Files.isRegularFile(storedFile)
        ).isTrue();

        assertThat(
                Files.readAllBytes(storedFile)
        ).isEqualTo(content);
    }

    @Test
    void failedWriteRemovesTemporaryFile()
            throws Exception {
        Path rootDirectory =
                temporaryDirectory.resolve(
                        "failed-write-root"
                );

        LocalObjectStorage storage =
                new LocalObjectStorage(
                        rootDirectory
                );

        assertThatThrownBy(
                () -> storage.put(
                        "documents/12/failure.bin",
                        new FailingInputStream()
                )
        )
                .isInstanceOf(
                        ObjectStorageException.class
                )
                .hasMessageContaining(
                        "Could not store object"
                );

        try (
            Stream<Path> paths =
                    Files.walk(rootDirectory)
        ) {
            List<Path> regularFiles =
                    paths
                            .filter(Files::isRegularFile)
                            .toList();

            assertThat(regularFiles).isEmpty();
        }
    }

    @Test
    void failingInputStreamSupportsSmallReadBuffers()
            throws Exception {
        FailingInputStream inputStream =
                new FailingInputStream();

        byte[] buffer = new byte[1];

        assertThat(inputStream.read(buffer))
                .isEqualTo(1);
        assertThat(buffer)
                .containsExactly((byte) 1);

        assertThat(inputStream.read(buffer))
                .isEqualTo(1);
        assertThat(buffer)
                .containsExactly((byte) 2);

        assertThat(inputStream.read(buffer))
                .isEqualTo(1);
        assertThat(buffer)
                .containsExactly((byte) 3);

        assertThatThrownBy(
                () -> inputStream.read(buffer)
        )
                .isInstanceOf(IOException.class)
                .hasMessage(
                        "Simulated read failure."
                );
    }

    @Test
    void rejectsFileAsStorageRoot()
            throws Exception {
        Path rootFile =
                temporaryDirectory.resolve(
                        "root-is-a-file"
                );

        Files.writeString(
                rootFile,
                "not a directory",
                StandardCharsets.UTF_8
        );

        assertThatThrownBy(
                () -> new LocalObjectStorage(rootFile)
        )
                .isInstanceOf(
                        ObjectStorageException.class
                )
                .hasMessageContaining(
                        "Could not initialize local object storage"
                );
    }

    private static final class FailingInputStream
            extends InputStream {

        private static final byte[] PREFIX =
                new byte[] {
                    1,
                    2,
                    3
                };

        private int nextPrefixIndex;

        @Override
        public int read() throws IOException {
            if (nextPrefixIndex < PREFIX.length) {
                return Byte.toUnsignedInt(
                        PREFIX[nextPrefixIndex++]
                );
            }

            throw simulatedFailure();
        }

        @Override
        public int read(
                byte[] buffer,
                int offset,
                int length
        ) throws IOException {
            Objects.checkFromIndexSize(
                    offset,
                    length,
                    buffer.length
            );

            if (length == 0) {
                return 0;
            }

            if (nextPrefixIndex >= PREFIX.length) {
                throw simulatedFailure();
            }

            int bytesToCopy = Math.min(
                    length,
                    PREFIX.length - nextPrefixIndex
            );

            System.arraycopy(
                    PREFIX,
                    nextPrefixIndex,
                    buffer,
                    offset,
                    bytesToCopy
            );

            nextPrefixIndex += bytesToCopy;

            return bytesToCopy;
        }

        private IOException simulatedFailure() {
            return new IOException(
                    "Simulated read failure."
            );
        }
    }
}
