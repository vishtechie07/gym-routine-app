package com.gymtracker.service;

import com.gymtracker.dto.WorkoutLogRequest;
import com.gymtracker.entity.WorkoutLog;
import com.gymtracker.repository.WorkoutLogRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

@Service
@Transactional
public class WorkoutLogService {

    private final WorkoutLogRepository workoutLogRepository;
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    @Autowired
    public WorkoutLogService(WorkoutLogRepository workoutLogRepository) {
        this.workoutLogRepository = workoutLogRepository;
    }

    public WorkoutLog createWorkoutLog(WorkoutLogRequest request) {
        // Parse the date string to LocalDate
        LocalDate workoutDate;
        try {
            workoutDate = LocalDate.parse(request.getDate(), DATE_FORMATTER);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("Invalid date format. Date must be in YYYY-MM-DD format.");
        }

        // Create the workout log entity
        WorkoutLog workoutLog = new WorkoutLog(
                request.getUserId(),
                workoutDate,
                request.getExerciseName(),
                request.getSets(),
                request.getReps(),
                request.getWeight()
        );

        // Save and return the workout log
        return workoutLogRepository.save(workoutLog);
    }
}
