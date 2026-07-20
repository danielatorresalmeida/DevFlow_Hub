package com.devflowhub.backend.dto;

import java.util.List;

public record DashboardSummary(
        long collaboratorCount,
        long projectCount,
        long taskCount,
        long programCount,
        long pendingTaskCount,
        long inProgressTaskCount,
        long reviewTaskCount,
        long completedTaskCount,
        long trackedTimeSeconds,
        List<DashboardTaskItem> recentTasks,
        List<DashboardProjectItem> upcomingProjects
) {
}
