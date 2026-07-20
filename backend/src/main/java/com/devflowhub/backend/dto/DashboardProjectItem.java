package com.devflowhub.backend.dto;

import java.time.LocalDate;

public record DashboardProjectItem(
        Long id,
        String name,
        String status,
        LocalDate endDate,
        String managerName
) {
}
