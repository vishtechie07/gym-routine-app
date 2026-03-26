package com.gymtracker.service;

import com.gymtracker.dto.MealLogRequest;
import com.gymtracker.entity.MealLog;
import com.gymtracker.repository.MealLogRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class MealLogService {

    private final MealLogRepository mealLogRepository;
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    @Autowired
    public MealLogService(MealLogRepository mealLogRepository) {
        this.mealLogRepository = mealLogRepository;
    }

    public MealLog createMealLog(MealLogRequest request, Long userId) {
        LocalDate mealDate;
        try {
            mealDate = LocalDate.parse(request.getDate(), DATE_FORMATTER);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("Invalid date format. Date must be in YYYY-MM-DD format.");
        }

        LocalTime time = parseMealTimeOrNow(request.getMealTime());

        MealLog mealLog = new MealLog(
                userId,
                mealDate,
                request.getMealName().trim(),
                request.getCalories(),
                request.getProtein(),
                request.getCarbs(),
                request.getFats(),
                time
        );

        return mealLogRepository.save(mealLog);
    }

    public List<MealLog> listMealsForDate(Long userId, LocalDate date) {
        return mealLogRepository.findByUserIdAndDateOrderByMealTimeAscIdAsc(userId, date);
    }

    public Optional<MealLog> updateMealLog(Long id, MealLogRequest request, Long userId) {
        return mealLogRepository.findByIdAndUserId(id, userId).map(meal -> {
            LocalDate mealDate;
            try {
                mealDate = LocalDate.parse(request.getDate(), DATE_FORMATTER);
            } catch (DateTimeParseException e) {
                throw new IllegalArgumentException("Invalid date format. Date must be in YYYY-MM-DD format.");
            }
            meal.setDate(mealDate);
            meal.setMealName(request.getMealName().trim());
            meal.setCalories(request.getCalories());
            meal.setProtein(request.getProtein());
            meal.setCarbs(request.getCarbs());
            meal.setFats(request.getFats());
            meal.setMealTime(parseMealTimeForUpdate(request.getMealTime(), meal.getMealTime()));
            return mealLogRepository.save(meal);
        });
    }

    public boolean deleteMealLog(Long id, Long userId) {
        return mealLogRepository.findByIdAndUserId(id, userId)
                .map(m -> {
                    mealLogRepository.delete(m);
                    return true;
                })
                .orElse(false);
    }

    private static LocalTime parseMealTimeForUpdate(String raw, LocalTime existing) {
        if (raw == null || raw.isBlank()) {
            return existing;
        }
        try {
            return LocalTime.parse(raw.trim(), DateTimeFormatter.ofPattern("HH:mm"));
        } catch (DateTimeParseException e) {
            return existing != null ? existing : LocalTime.now();
        }
    }

    private static LocalTime parseMealTimeOrNow(String raw) {
        if (raw == null || raw.isBlank()) {
            return LocalTime.now();
        }
        try {
            return LocalTime.parse(raw.trim(), DateTimeFormatter.ofPattern("HH:mm"));
        } catch (DateTimeParseException e) {
            return LocalTime.now();
        }
    }
}
