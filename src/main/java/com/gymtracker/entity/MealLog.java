package com.gymtracker.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

@Entity
@Table(name = "meal_logs")
public class MealLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull(message = "User ID is required")
    @Column(name = "user_id", nullable = false)
    private Long userId;

    @NotNull(message = "Date is required")
    @Column(name = "date", nullable = false)
    private LocalDate date;

    @NotBlank(message = "Meal name is required")
    @Size(min = 1, max = 100, message = "Meal name must be between 1 and 100 characters")
    @Column(name = "meal_name", nullable = false, length = 100)
    private String mealName;

    @NotNull(message = "Calories are required")
    @Min(value = 0, message = "Calories must be non-negative")
    @Max(value = 10000, message = "Calories cannot exceed 10000")
    @Column(name = "calories", nullable = false)
    private Integer calories;

    @NotNull(message = "Protein is required")
    @DecimalMin(value = "0.0", inclusive = true, message = "Protein must be non-negative")
    @Digits(integer = 4, fraction = 1, message = "Protein can have up to 4 digits before decimal and 1 after")
    @Column(name = "protein", nullable = false, precision = 5, scale = 1)
    private BigDecimal protein;

    @NotNull(message = "Carbs are required")
    @DecimalMin(value = "0.0", inclusive = true, message = "Carbs must be non-negative")
    @Digits(integer = 4, fraction = 1, message = "Carbs can have up to 4 digits before decimal and 1 after")
    @Column(name = "carbs", nullable = false, precision = 5, scale = 1)
    private BigDecimal carbs;

    @NotNull(message = "Fats are required")
    @DecimalMin(value = "0.0", inclusive = true, message = "Fats must be non-negative")
    @Digits(integer = 4, fraction = 1, message = "Fats can have up to 4 digits before decimal and 1 after")
    @Column(name = "fats", nullable = false, precision = 5, scale = 1)
    private BigDecimal fats;

    @Column(name = "meal_time")
    private LocalTime mealTime;

    public MealLog() {}

    public MealLog(Long userId, LocalDate date, String mealName, Integer calories, BigDecimal protein, BigDecimal carbs, BigDecimal fats, LocalTime mealTime) {
        this.userId = userId;
        this.date = date;
        this.mealName = mealName;
        this.calories = calories;
        this.protein = protein;
        this.carbs = carbs;
        this.fats = fats;
        this.mealTime = mealTime;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
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

    public LocalTime getMealTime() {
        return mealTime;
    }

    public void setMealTime(LocalTime mealTime) {
        this.mealTime = mealTime;
    }

    @Override
    public String toString() {
        return "MealLog{" +
                "id=" + id +
                ", userId=" + userId +
                ", date=" + date +
                ", mealName='" + mealName + '\'' +
                ", calories=" + calories +
                ", protein=" + protein +
                ", carbs=" + carbs +
                ", fats=" + fats +
                ", mealTime=" + mealTime +
                '}';
    }
}
