package com.devflowhub.backend.storage;

import java.io.InputStream;
import java.util.Optional;

public interface ObjectStorage {

    ObjectStorageMetadata put(
            String key,
            InputStream content
    );

    ObjectStorageObject get(String key);

    Optional<ObjectStorageMetadata> findMetadata(
            String key
    );

    boolean exists(String key);

    void delete(String key);
}
