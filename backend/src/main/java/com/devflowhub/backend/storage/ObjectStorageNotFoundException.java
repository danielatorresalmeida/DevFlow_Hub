package com.devflowhub.backend.storage;

public final class ObjectStorageNotFoundException
        extends ObjectStorageException {

    public ObjectStorageNotFoundException(String key) {
        super("Object not found: " + key);
    }
}
