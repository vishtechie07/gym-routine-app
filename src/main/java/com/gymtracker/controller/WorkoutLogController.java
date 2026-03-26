package com.gymtracker.controller;

import com.gymtracker.dto.WorkoutLogRequest;
import com.gymtracker.entity.WorkoutLog;
import com.gymtracker.security.SecurityUtil;
import com.gymtracker.service.WorkoutLogService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/workouts")
@CrossOrigin(origins = "*")
public class WorkoutLogController {

    private final WorkoutLogService workoutLogService;

    @Autowired
    public WorkoutLogController(WorkoutLogService workoutLogService) {
        this.workoutLogService = workoutLogService;
    }

    @PostMapping("/log")
    public ResponseEntity<WorkoutLog> logWorkout(@Valid @RequestBody WorkoutLogRequest request) {
        try {
            // Additional security validation
            if (request.getExerciseName() != null) {
                String exerciseName = request.getExerciseName().trim();
                // Check for XSS patterns
                if (exerciseName.contains("<script") || exerciseName.contains("javascript:") || 
                    exerciseName.contains("onload=") || exerciseName.contains("onerror=")) {
                    return ResponseEntity.badRequest().body(null);
                }
            }
            
            WorkoutLog createdWorkout = workoutLogService.createWorkoutLog(request, SecurityUtil.currentUserId());
            return ResponseEntity.status(HttpStatus.CREATED).body(createdWorkout);
        } catch (IllegalArgumentException e) {
            throw e;
        }
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> handleValidationExceptions(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach((error) -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });
        return ResponseEntity.badRequest().body(errors);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleIllegalArgumentException(IllegalArgumentException ex) {
        Map<String, String> error = new HashMap<>();
        error.put("error", ex.getMessage());
        return ResponseEntity.badRequest().body(error);
    }
}
