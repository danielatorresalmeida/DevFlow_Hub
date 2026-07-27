package com.devflowhub.backend.storage;

import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;

public record ObjectStorageObject(
        ObjectStorageMetadata metadata,
        InputStream content
) implements AutoCloseable {

    public ObjectStorageObject {
        Objects.requireNonNull(
                metadata,
                "Object metadata is required."
        );

        Objects.requireNonNull(
                content,
                "Object content is required."
        );
    }

    @Override
    public void close() throws IOException {
        content.close();
    }
}
