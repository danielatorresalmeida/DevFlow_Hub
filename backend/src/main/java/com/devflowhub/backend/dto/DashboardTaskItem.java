package com.devflowhub.backend.dto;

import java.time.LocalDateTime;

public record DashboardTaskItem(
        Long id,
        String title,
        String status,
        String priority,
        LocalDateTime updatedAt,
        String assigneeName
) {
}
