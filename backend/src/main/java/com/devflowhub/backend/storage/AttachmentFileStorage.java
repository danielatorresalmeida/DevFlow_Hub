package com.devflowhub.backend.storage;

import com.devflowhub.backend.config.AttachmentStorageProperties;
import com.devflowhub.backend.exception.FileStorageException;
import com.devflowhub.backend.exception.InvalidOperationException;
import com.devflowhub.backend.exception.ResourceNotFoundException;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

@Service
public class AttachmentFileStorage {

    private final Path rootDirectory;

    public AttachmentFileStorage(
            AttachmentStorageProperties properties
    ) {
        rootDirectory = properties
                .getRoot()
                .toAbsolutePath()
                .normalize();
    }

    @PostConstruct
    public void initialize() {
        try {
            Files.createDirectories(rootDirectory);

            if (!Files.isDirectory(rootDirectory)) {
                throw new FileStorageException(
                        "The attachment storage location is not a directory.",
                        null
                );
            }
        }
        catch (IOException exception) {
            throw new FileStorageException(
                    "The attachment storage location could not be initialized.",
                    exception
            );
        }
    }

    public void store(
            String storageKey,
            InputStream inputStream
    ) {
        if (inputStream == null) {
            throw new InvalidOperationException(
                    "File content is required."
            );
        }

        Path target = resolveStorageKey(storageKey);
        Path temporaryFile = null;

        try {
            Files.createDirectories(target.getParent());

            temporaryFile = Files.createTempFile(
                    target.getParent(),
                    ".upload-",
                    ".tmp"
            );

            Files.copy(
                    inputStream,
                    temporaryFile,
                    StandardCopyOption.REPLACE_EXISTING
            );

            moveIntoPlace(temporaryFile, target);
            temporaryFile = null;
        }
        catch (FileAlreadyExistsException exception) {
            throw new InvalidOperationException(
                    "The storage key is already in use."
            );
        }
        catch (IOException exception) {
            throw new FileStorageException(
                    "The attachment file could not be stored.",
                    exception
            );
        }
        finally {
            deleteTemporaryFile(temporaryFile);
        }
    }

    public Path load(String storageKey) {
        Path target = resolveStorageKey(storageKey);

        if (!Files.isRegularFile(target)) {
            throw new ResourceNotFoundException(
                    "Attachment file not found."
            );
        }

        return target;
    }

    public boolean exists(String storageKey) {
        return Files.isRegularFile(
                resolveStorageKey(storageKey)
        );
    }

    public void delete(String storageKey) {
        Path target = resolveStorageKey(storageKey);

        try {
            Files.deleteIfExists(target);
        }
        catch (IOException exception) {
            throw new FileStorageException(
                    "The attachment file could not be deleted.",
                    exception
            );
        }
    }

    private Path resolveStorageKey(String storageKey) {
        if (
                storageKey == null
                        || storageKey.isBlank()
        ) {
            throw new InvalidOperationException(
                    "Storage key is required."
            );
        }

        try {
            Path relativePath = Path
                    .of(storageKey.trim())
                    .normalize();

            if (
                    relativePath.isAbsolute()
                            || relativePath.getNameCount() == 0
                            || relativePath.startsWith("..")
            ) {
                throw invalidStorageKey();
            }

            Path target = rootDirectory
                    .resolve(relativePath)
                    .normalize();

            if (!target.startsWith(rootDirectory)) {
                throw invalidStorageKey();
            }

            return target;
        }
        catch (InvalidPathException exception) {
            throw invalidStorageKey();
        }
    }

    private InvalidOperationException invalidStorageKey() {
        return new InvalidOperationException(
                "The storage key is invalid."
        );
    }

    private void moveIntoPlace(
            Path temporaryFile,
            Path target
    ) throws IOException {
        Files.move(temporaryFile, target);
    }

    private void deleteTemporaryFile(Path temporaryFile) {
        if (temporaryFile == null) {
            return;
        }

        try {
            Files.deleteIfExists(temporaryFile);
        }
        catch (IOException ignored) {
            // The original storage error remains the primary failure.
        }
    }
}