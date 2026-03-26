package com.gymtracker.dto;

import java.util.List;

public record DashboardSummaryDto(
        int workoutsThisWeek,
        double totalVolumeLb,
        int avgCaloriesPerDayLast7,
        boolean aiConfigured,
        List<RecentActivityDto> recent
) {
    public record RecentActivityDto(
            String type,
            String title,
            String subtitle,
            String meta,
            String date
    ) {}
}
