package com.gymtracker.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;

public class WorkoutLogRequest {

    @NotBlank(message = "Date is required")
    @Pattern(regexp = "\\d{4}-\\d{2}-\\d{2}", message = "Date must be in YYYY-MM-DD format")
    private String date;

    @NotBlank(message = "Exercise name is required")
    @Size(min = 1, max = 100, message = "Exercise name must be between 1 and 100 characters")
    private String exerciseName;

    @NotNull(message = "Number of sets is required")
    @Min(value = 1, message = "Sets must be at least 1")
    @Max(value = 100, message = "Sets cannot exceed 100")
    private Integer sets;

    @NotNull(message = "Number of reps is required")
    @Min(value = 1, message = "Reps must be at least 1")
    @Max(value = 1000, message = "Reps cannot exceed 1000")
    private Integer reps;

    @NotNull(message = "Weight is required")
    @DecimalMin(value = "0.0", inclusive = false, message = "Weight must be greater than 0")
    @Digits(integer = 5, fraction = 2, message = "Weight can have up to 5 digits before decimal and 2 after")
    private BigDecimal weight;

    // Default constructor
    public WorkoutLogRequest() {}

    // Constructor with all fields
    public WorkoutLogRequest(String date, String exerciseName, Integer sets, Integer reps, BigDecimal weight) {
        this.date = date;
        this.exerciseName = exerciseName;
        this.sets = sets;
        this.reps = reps;
        this.weight = weight;
    }

    // Getters and Setters
    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public String getExerciseName() {
        return exerciseName;
    }

    public void setExerciseName(String exerciseName) {
        this.exerciseName = exerciseName;
    }

    public Integer getSets() {
        return sets;
    }

    public void setSets(Integer sets) {
        this.sets = sets;
    }

    public Integer getReps() {
        return reps;
    }

    public void setReps(Integer reps) {
        this.reps = reps;
    }

    public BigDecimal getWeight() {
        return weight;
    }

    public void setWeight(BigDecimal weight) {
        this.weight = weight;
    }
}
