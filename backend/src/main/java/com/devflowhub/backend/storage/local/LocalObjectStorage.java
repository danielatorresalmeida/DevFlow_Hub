package com.devflowhub.backend.storage.local;

import com.devflowhub.backend.storage.ObjectStorage;
import com.devflowhub.backend.storage.ObjectStorageException;
import com.devflowhub.backend.storage.ObjectStorageMetadata;
import com.devflowhub.backend.storage.ObjectStorageNotFoundException;
import com.devflowhub.backend.storage.ObjectStorageObject;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Optional;
import java.util.regex.Pattern;

public class LocalObjectStorage implements ObjectStorage {

    private static final int BUFFER_SIZE = 8192;

    private static final Pattern SAFE_KEY_PATTERN =
            Pattern.compile(
                    "^[A-Za-z0-9][A-Za-z0-9._/-]{0,1023}$"
            );

    private final Path rootDirectory;

    public LocalObjectStorage(Path rootDirectory) {
        if (rootDirectory == null) {
            throw new ObjectStorageException(
                    "Storage root directory is required."
            );
        }

        try {
            Path normalizedRoot =
                    rootDirectory
                            .toAbsolutePath()
                            .normalize();

            Files.createDirectories(normalizedRoot);

            if (
                Files.isSymbolicLink(normalizedRoot) ||
                !Files.isDirectory(
                        normalizedRoot,
                        LinkOption.NOFOLLOW_LINKS
                )
            ) {
                throw new ObjectStorageException(
                        "Storage root must be a directory."
                );
            }

            this.rootDirectory =
                    normalizedRoot.toRealPath();
        }
        catch (IOException exception) {
            throw new ObjectStorageException(
                    "Could not initialize local object storage.",
                    exception
            );
        }
    }

    @Override
    public ObjectStorageMetadata put(
            String key,
            InputStream content
    ) {
        String validatedKey = validateKey(key);

        if (content == null) {
            throw new ObjectStorageException(
                    "Object content is required."
            );
        }

        Path temporaryFile = null;

        try {
            Path target =
                    prepareWritablePath(validatedKey);

            temporaryFile = Files.createTempFile(
                    target.getParent(),
                    ".object-upload-",
                    ".tmp"
            );

            MessageDigest messageDigest =
                    createSha256Digest();

            long sizeBytes = 0L;

            try (
                OutputStream outputStream =
                        Files.newOutputStream(
                                temporaryFile,
                                StandardOpenOption.WRITE,
                                StandardOpenOption.TRUNCATE_EXISTING
                        )
            ) {
                byte[] buffer = new byte[BUFFER_SIZE];
                int bytesRead;

                while (
                    (
                        bytesRead =
                                content.read(buffer)
                    ) != -1
                ) {
                    if (bytesRead == 0) {
                        continue;
                    }

                    outputStream.write(
                            buffer,
                            0,
                            bytesRead
                    );

                    messageDigest.update(
                            buffer,
                            0,
                            bytesRead
                    );

                    sizeBytes += bytesRead;
                }
            }

            String sha256 =
                    HexFormat.of().formatHex(
                            messageDigest.digest()
                    );

            moveIntoPlace(
                    temporaryFile,
                    target
            );

            temporaryFile = null;

            return createMetadata(
                    validatedKey,
                    target,
                    sizeBytes,
                    sha256
            );
        }
        catch (IOException exception) {
            cleanupTemporaryFile(
                    temporaryFile,
                    exception
            );

            throw new ObjectStorageException(
                    "Could not store object '" +
                    validatedKey +
                    "'.",
                    exception
            );
        }
        catch (RuntimeException exception) {
            cleanupTemporaryFile(
                    temporaryFile,
                    exception
            );

            throw exception;
        }
    }

    @Override
    public ObjectStorageObject get(String key) {
        String validatedKey = validateKey(key);
        Path target = resolveObjectPath(validatedKey);

        if (
            !Files.isRegularFile(
                    target,
                    LinkOption.NOFOLLOW_LINKS
            )
        ) {
            throw new ObjectStorageNotFoundException(
                    validatedKey
            );
        }

        try {
            ObjectStorageMetadata metadata =
                    readMetadata(
                            validatedKey,
                            target
                    );

            InputStream content =
                    Files.newInputStream(
                            target,
                            StandardOpenOption.READ
                    );

            return new ObjectStorageObject(
                    metadata,
                    content
            );
        }
        catch (IOException exception) {
            throw new ObjectStorageException(
                    "Could not read object '" +
                    validatedKey +
                    "'.",
                    exception
            );
        }
    }

    @Override
    public Optional<ObjectStorageMetadata> findMetadata(
            String key
    ) {
        String validatedKey = validateKey(key);
        Path target = resolveObjectPath(validatedKey);

        if (
            !Files.isRegularFile(
                    target,
                    LinkOption.NOFOLLOW_LINKS
            )
        ) {
            return Optional.empty();
        }

        try {
            return Optional.of(
                    readMetadata(
                            validatedKey,
                            target
                    )
            );
        }
        catch (IOException exception) {
            throw new ObjectStorageException(
                    "Could not read metadata for object '" +
                    validatedKey +
                    "'.",
                    exception
            );
        }
    }

    @Override
    public boolean exists(String key) {
        String validatedKey = validateKey(key);
        Path target = resolveObjectPath(validatedKey);

        return Files.isRegularFile(
                target,
                LinkOption.NOFOLLOW_LINKS
        );
    }

    @Override
    public void delete(String key) {
        String validatedKey = validateKey(key);
        Path target = resolveObjectPath(validatedKey);

        if (
            Files.exists(
                    target,
                    LinkOption.NOFOLLOW_LINKS
            ) &&
            !Files.isRegularFile(
                    target,
                    LinkOption.NOFOLLOW_LINKS
            )
        ) {
            throw new ObjectStorageException(
                    "Object key does not identify a regular file."
            );
        }

        try {
            Files.deleteIfExists(target);
        }
        catch (IOException exception) {
            throw new ObjectStorageException(
                    "Could not delete object '" +
                    validatedKey +
                    "'.",
                    exception
            );
        }
    }

    private String validateKey(String key) {
        if (
            key == null ||
            key.isBlank() ||
            key.length() > 1024 ||
            !SAFE_KEY_PATTERN.matcher(key).matches() ||
            key.startsWith("/") ||
            key.endsWith("/") ||
            key.contains("//")
        ) {
            throw new ObjectStorageException(
                    "Invalid object key."
            );
        }

        String[] segments = key.split("/");

        for (String segment : segments) {
            if (
                segment.equals(".") ||
                segment.equals("..")
            ) {
                throw new ObjectStorageException(
                        "Invalid object key."
                );
            }
        }

        return key;
    }

    private Path prepareWritablePath(
            String validatedKey
    ) throws IOException {
        String[] segments = validatedKey.split("/");
        Path currentDirectory = rootDirectory;

        for (
            int index = 0;
            index < segments.length - 1;
            index++
        ) {
            Path nextDirectory =
                    currentDirectory.resolve(
                            segments[index]
                    );

            if (
                Files.exists(
                        nextDirectory,
                        LinkOption.NOFOLLOW_LINKS
                )
            ) {
                if (
                    Files.isSymbolicLink(nextDirectory) ||
                    !Files.isDirectory(
                            nextDirectory,
                            LinkOption.NOFOLLOW_LINKS
                    )
                ) {
                    throw new ObjectStorageException(
                            "Object key crosses an unsafe path."
                    );
                }
            }
            else {
                Files.createDirectory(nextDirectory);
            }

            currentDirectory = nextDirectory;
        }

        Path target =
                currentDirectory.resolve(
                        segments[
                                segments.length - 1
                        ]
                );

        if (Files.isSymbolicLink(target)) {
            throw new ObjectStorageException(
                    "Object key crosses an unsafe path."
            );
        }

        if (
            Files.exists(
                    target,
                    LinkOption.NOFOLLOW_LINKS
            ) &&
            !Files.isRegularFile(
                    target,
                    LinkOption.NOFOLLOW_LINKS
            )
        ) {
            throw new ObjectStorageException(
                    "Object key does not identify a regular file."
            );
        }

        return target;
    }

    private Path resolveObjectPath(
            String validatedKey
    ) {
        String[] segments = validatedKey.split("/");
        Path currentPath = rootDirectory;

        for (String segment : segments) {
            currentPath = currentPath.resolve(segment);

            if (Files.isSymbolicLink(currentPath)) {
                throw new ObjectStorageException(
                        "Object key crosses an unsafe path."
                );
            }
        }

        Path normalizedPath =
                currentPath.normalize();

        if (!normalizedPath.startsWith(rootDirectory)) {
            throw new ObjectStorageException(
                    "Object key escapes the storage root."
            );
        }

        return normalizedPath;
    }

    private ObjectStorageMetadata readMetadata(
            String key,
            Path target
    ) throws IOException {
        MessageDigest messageDigest =
                createSha256Digest();

        long sizeBytes = 0L;

        try (
            InputStream inputStream =
                    Files.newInputStream(
                            target,
                            StandardOpenOption.READ
                    )
        ) {
            byte[] buffer = new byte[BUFFER_SIZE];
            int bytesRead;

            while (
                (
                    bytesRead =
                            inputStream.read(buffer)
                ) != -1
            ) {
                if (bytesRead == 0) {
                    continue;
                }

                messageDigest.update(
                        buffer,
                        0,
                        bytesRead
                );

                sizeBytes += bytesRead;
            }
        }

        return createMetadata(
                key,
                target,
                sizeBytes,
                HexFormat.of().formatHex(
                        messageDigest.digest()
                )
        );
    }

    private ObjectStorageMetadata createMetadata(
            String key,
            Path target,
            long expectedSize,
            String sha256
    ) throws IOException {
        BasicFileAttributes attributes =
                Files.readAttributes(
                        target,
                        BasicFileAttributes.class,
                        LinkOption.NOFOLLOW_LINKS
                );

        if (
            !attributes.isRegularFile() ||
            attributes.size() != expectedSize
        ) {
            throw new IOException(
                    "Object changed while metadata was being read."
            );
        }

        return new ObjectStorageMetadata(
                key,
                expectedSize,
                sha256,
                sha256
        );
    }

    private MessageDigest createSha256Digest() {
        try {
            return MessageDigest.getInstance(
                    "SHA-256"
            );
        }
        catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException(
                    "SHA-256 is not available.",
                    exception
            );
        }
    }

    private void moveIntoPlace(
            Path temporaryFile,
            Path target
    ) throws IOException {
        try {
            Files.move(
                    temporaryFile,
                    target,
                    StandardCopyOption.ATOMIC_MOVE,
                    StandardCopyOption.REPLACE_EXISTING
            );
        }
        catch (
            AtomicMoveNotSupportedException exception
        ) {
            Files.move(
                    temporaryFile,
                    target,
                    StandardCopyOption.REPLACE_EXISTING
            );
        }
    }

    private void cleanupTemporaryFile(
            Path temporaryFile,
            Throwable failure
    ) {
        if (temporaryFile == null) {
            return;
        }

        try {
            Files.deleteIfExists(temporaryFile);
        }
        catch (IOException cleanupException) {
            failure.addSuppressed(cleanupException);
        }
    }
}
