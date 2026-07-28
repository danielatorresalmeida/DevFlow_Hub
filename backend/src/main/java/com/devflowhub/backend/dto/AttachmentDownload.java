package com.devflowhub.backend.dto;

import com.devflowhub.backend.entity.Attachment;
import org.springframework.core.io.Resource;

public record AttachmentDownload(
        Attachment attachment,
        Resource resource
) {
}