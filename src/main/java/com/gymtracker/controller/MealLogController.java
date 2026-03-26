package com.gymtracker.controller;

import com.gymtracker.dto.MealLogRequest;
import com.gymtracker.entity.MealLog;
import com.gymtracker.security.SecurityUtil;
import com.gymtracker.service.MealLogService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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
        if (hasMealNameXss(request)) {
            return ResponseEntity.badRequest().body(null);
        }
        try {
            MealLog createdMeal = mealLogService.createMealLog(request, SecurityUtil.currentUserId());
            return ResponseEntity.status(HttpStatus.CREATED).body(createdMeal);
        } catch (IllegalArgumentException e) {
            throw e;
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<MealLog> updateMeal(@PathVariable Long id, @Valid @RequestBody MealLogRequest request) {
        if (id == null || id <= 0) {
            return ResponseEntity.badRequest().build();
        }
        if (hasMealNameXss(request)) {
            return ResponseEntity.badRequest().build();
        }
        try {
            return mealLogService.updateMealLog(id, request, SecurityUtil.currentUserId())
                    .map(ResponseEntity::ok)
                    .orElse(ResponseEntity.notFound().build());
        } catch (IllegalArgumentException e) {
            throw e;
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteMeal(@PathVariable Long id) {
        if (id == null || id <= 0) {
            return ResponseEntity.badRequest().build();
        }
        if (!mealLogService.deleteMealLog(id, SecurityUtil.currentUserId())) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.noContent().build();
    }

    private static boolean hasMealNameXss(MealLogRequest request) {
        if (request.getMealName() == null) {
            return false;
        }
        String mealName = request.getMealName().trim();
        return mealName.contains("<script") || mealName.contains("javascript:")
                || mealName.contains("onload=") || mealName.contains("onerror=");
    }

    private static final DateTimeFormatter MEAL_LIST_DATE = DateTimeFormatter.ISO_LOCAL_DATE;

    @GetMapping("/list")
    public ResponseEntity<List<Map<String, Object>>> listMeals(@RequestParam(required = false) String date) {
        LocalDate d;
        try {
            d = date == null || date.isBlank() ? LocalDate.now() : LocalDate.parse(date.trim(), MEAL_LIST_DATE);
        } catch (DateTimeParseException e) {
            return ResponseEntity.badRequest().body(List.of());
        }
        return ResponseEntity.ok(
                mealLogService.listMealsForDate(SecurityUtil.currentUserId(), d).stream()
                        .map(this::mealToBrief)
                        .collect(Collectors.toList())
        );
    }

    private Map<String, Object> mealToBrief(MealLog m) {
        Map<String, Object> row = new HashMap<>();
        row.put("id", m.getId());
        row.put("date", m.getDate().toString());
        row.put("mealName", m.getMealName());
        row.put("calories", m.getCalories());
        row.put("protein", m.getProtein());
        row.put("carbs", m.getCarbs());
        row.put("fats", m.getFats());
        row.put("mealTime", m.getMealTime() != null ? m.getMealTime().toString() : null);
        return row;
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
