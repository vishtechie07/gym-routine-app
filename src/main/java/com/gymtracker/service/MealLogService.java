package com.gymtracker.service;

import com.gymtracker.dto.MealLogRequest;
import com.gymtracker.entity.MealLog;
import com.gymtracker.repository.MealLogRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

@Service
@Transactional
public class MealLogService {

    private final MealLogRepository mealLogRepository;
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    @Autowired
    public MealLogService(MealLogRepository mealLogRepository) {
        this.mealLogRepository = mealLogRepository;
    }

    public MealLog createMealLog(MealLogRequest request) {
        // Parse the date string to LocalDate
        LocalDate mealDate;
        try {
            mealDate = LocalDate.parse(request.getDate(), DATE_FORMATTER);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("Invalid date format. Date must be in YYYY-MM-DD format.");
        }

        // Create the meal log entity
        MealLog mealLog = new MealLog(
                request.getUserId(),
                mealDate,
                request.getMealName(),
                request.getCalories(),
                request.getProtein(),
                request.getCarbs(),
                request.getFats()
        );

        // Save and return the meal log
        return mealLogRepository.save(mealLog);
    }
}
