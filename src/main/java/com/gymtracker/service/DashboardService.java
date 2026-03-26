package com.gymtracker.service;

import com.gymtracker.dto.DashboardSummaryDto;
import com.gymtracker.dto.DashboardSummaryDto.RecentActivityDto;
import com.gymtracker.entity.MealLog;
import com.gymtracker.entity.WorkoutLog;
import com.gymtracker.repository.MealLogRepository;
import com.gymtracker.repository.WorkoutLogRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
public class DashboardService {

    private static final DateTimeFormatter ISO = DateTimeFormatter.ISO_LOCAL_DATE;

    private final WorkoutLogRepository workoutLogRepository;
    private final MealLogRepository mealLogRepository;
    private final AIService aiService;

    @Autowired
    public DashboardService(WorkoutLogRepository workoutLogRepository,
                            MealLogRepository mealLogRepository,
                            AIService aiService) {
        this.workoutLogRepository = workoutLogRepository;
        this.mealLogRepository = mealLogRepository;
        this.aiService = aiService;
    }

    public DashboardSummaryDto getSummary(Long userId) {
        if (userId == null || userId < 1) {
            throw new IllegalArgumentException("Invalid user id");
        }
        LocalDate today = LocalDate.now();
        LocalDate weekStart = today.with(DayOfWeek.MONDAY);
        LocalDate last7 = today.minusDays(6);

        List<WorkoutLog> workoutsWeek = workoutLogRepository
                .findByUserIdAndDateGreaterThanEqualOrderByDateDescIdDesc(userId, weekStart);
        List<MealLog> meals7 = mealLogRepository
                .findByUserIdAndDateGreaterThanEqualOrderByDateDescIdDesc(userId, last7);

        int workoutCount = workoutsWeek.size();
        double volume = workoutsWeek.stream()
                .mapToDouble(w -> w.getWeight().doubleValue() * w.getSets() * w.getReps())
                .sum();

        int avgCal = 0;
        if (!meals7.isEmpty()) {
            int totalCal = meals7.stream().mapToInt(MealLog::getCalories).sum();
            avgCal = Math.round((float) totalCal / 7f);
        }

        List<RecentActivityDto> recent = buildRecent(userId, today);

        return new DashboardSummaryDto(
                workoutCount,
                Math.round(volume * 10) / 10.0,
                avgCal,
                aiService.isAIConfigured(),
                recent
        );
    }

    private List<RecentActivityDto> buildRecent(Long userId, LocalDate today) {
        LocalDate from = today.minusDays(90);
        List<WorkoutLog> w = workoutLogRepository
                .findByUserIdAndDateGreaterThanEqualOrderByDateDescIdDesc(userId, from);
        List<MealLog> m = mealLogRepository
                .findByUserIdAndDateGreaterThanEqualOrderByDateDescIdDesc(userId, from);

        record Pair(LocalDate d, RecentActivityDto dto) {}
        List<Pair> pairs = new ArrayList<>();
        for (WorkoutLog x : w) {
            pairs.add(new Pair(x.getDate(), new RecentActivityDto(
                    "workout",
                    x.getExerciseName(),
                    x.getSets() + " sets × " + x.getReps() + " reps",
                    x.getWeight() + " lbs",
                    x.getDate().format(ISO)
            )));
        }
        for (MealLog x : m) {
            String timePart = x.getMealTime() != null ? x.getMealTime().toString() : "";
            pairs.add(new Pair(x.getDate(), new RecentActivityDto(
                    "meal",
                    x.getMealName(),
                    x.getCalories() + " kcal" + (timePart.isEmpty() ? "" : " · " + timePart),
                    "P " + x.getProtein() + "g",
                    x.getDate().format(ISO)
            )));
        }
        return pairs.stream()
                .sorted(Comparator.comparing(Pair::d).reversed())
                .limit(5)
                .map(Pair::dto)
                .toList();
    }
}
