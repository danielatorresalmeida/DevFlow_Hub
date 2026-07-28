package com.devflowhub.backend.config;

import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

import java.nio.file.Path;

@Component
@Validated
@ConfigurationProperties(prefix = "storage.attachments")
public class AttachmentStorageProperties {

    @NotNull
    private Path root = Path.of("./uploads");

    public Path getRoot() {
        return root;
    }

    public void setRoot(Path root) {
        this.root = root;
    }
}