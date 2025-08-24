package com.gymtracker.controller;

import com.gymtracker.dto.MealLogRequest;
import com.gymtracker.entity.MealLog;
import com.gymtracker.service.MealLogService;
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
@RequestMapping("/api/meals")
@CrossOrigin(origins = "*")
public class MealLogController {

    private final MealLogService mealLogService;

    @Autowired
    public MealLogController(MealLogService mealLogService) {
        this.mealLogService = mealLogService;
    }

    @PostMapping("/log")
    public ResponseEntity<MealLog> logMeal(@Valid @RequestBody MealLogRequest request) {
        try {
            MealLog createdMeal = mealLogService.createMealLog(request);
            return ResponseEntity.status(HttpStatus.CREATED).body(createdMeal);
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
