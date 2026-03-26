package com.gymtracker.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;

public class MealLogRequest {

    @NotBlank(message = "Date is required")
    @Pattern(regexp = "\\d{4}-\\d{2}-\\d{2}", message = "Date must be in YYYY-MM-DD format")
    private String date;

    @NotBlank(message = "Meal name is required")
    @Size(min = 1, max = 100, message = "Meal name must be between 1 and 100 characters")
    private String mealName;

    @NotNull(message = "Calories are required")
    @Min(value = 0, message = "Calories must be non-negative")
    @Max(value = 10000, message = "Calories cannot exceed 10000")
    private Integer calories;

    @NotNull(message = "Protein is required")
    @DecimalMin(value = "0.0", inclusive = true, message = "Protein must be non-negative")
    @Digits(integer = 4, fraction = 1, message = "Protein can have up to 4 digits before decimal and 1 after")
    private BigDecimal protein;

    @NotNull(message = "Carbs are required")
    @DecimalMin(value = "0.0", inclusive = true, message = "Carbs must be non-negative")
    @Digits(integer = 4, fraction = 1, message = "Carbs can have up to 4 digits before decimal and 1 after")
    private BigDecimal carbs;

    @NotNull(message = "Fats are required")
    @DecimalMin(value = "0.0", inclusive = true, message = "Fats must be non-negative")
    @Digits(integer = 4, fraction = 1, message = "Fats can have up to 4 digits before decimal and 1 after")
    private BigDecimal fats;

    @Pattern(regexp = "^$|^([01]\\d|2[0-3]):[0-5]\\d$", message = "mealTime must be HH:mm (24-hour)")
    private String mealTime;

    public MealLogRequest() {}

    public MealLogRequest(String date, String mealName, Integer calories, BigDecimal protein, BigDecimal carbs, BigDecimal fats) {
        this.date = date;
        this.mealName = mealName;
        this.calories = calories;
        this.protein = protein;
        this.carbs = carbs;
        this.fats = fats;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public String getMealName() {
        return mealName;
    }

    public void setMealName(String mealName) {
        this.mealName = mealName;
    }

    public Integer getCalories() {
        return calories;
    }

    public void setCalories(Integer calories) {
        this.calories = calories;
    }

    public BigDecimal getProtein() {
        return protein;
    }

    public void setProtein(BigDecimal protein) {
        this.protein = protein;
    }

    public BigDecimal getCarbs() {
        return carbs;
    }

    public void setCarbs(BigDecimal carbs) {
        this.carbs = carbs;
    }

    public BigDecimal getFats() {
        return fats;
    }

    public void setFats(BigDecimal fats) {
        this.fats = fats;
    }

    public String getMealTime() {
        return mealTime;
    }

    public void setMealTime(String mealTime) {
        this.mealTime = mealTime;
    }
}
