package com.gymtracker.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "workout_logs")
public class WorkoutLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull(message = "User ID is required")
    @Min(value = 1, message = "User ID must be a positive integer")
    @Column(name = "user_id", nullable = false)
    private Integer userId;

    @NotNull(message = "Date is required")
    @Column(name = "date", nullable = false)
    private LocalDate date;

    @NotBlank(message = "Exercise name is required")
    @Size(min = 1, max = 100, message = "Exercise name must be between 1 and 100 characters")
    @Column(name = "exercise_name", nullable = false, length = 100)
    private String exerciseName;

    @NotNull(message = "Number of sets is required")
    @Min(value = 1, message = "Sets must be at least 1")
    @Max(value = 100, message = "Sets cannot exceed 100")
    @Column(name = "sets", nullable = false)
    private Integer sets;

    @NotNull(message = "Number of reps is required")
    @Min(value = 1, message = "Reps must be at least 1")
    @Max(value = 1000, message = "Reps cannot exceed 1000")
    @Column(name = "reps", nullable = false)
    private Integer reps;

    @NotNull(message = "Weight is required")
    @DecimalMin(value = "0.0", inclusive = false, message = "Weight must be greater than 0")
    @Digits(integer = 5, fraction = 2, message = "Weight can have up to 5 digits before decimal and 2 after")
    @Column(name = "weight", nullable = false, precision = 7, scale = 2)
    private BigDecimal weight;

    // Default constructor
    public WorkoutLog() {}

    // Constructor with all fields
    public WorkoutLog(Integer userId, LocalDate date, String exerciseName, Integer sets, Integer reps, BigDecimal weight) {
        this.userId = userId;
        this.date = date;
        this.exerciseName = exerciseName;
        this.sets = sets;
        this.reps = reps;
        this.weight = weight;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Integer getUserId() {
        return userId;
    }

    public void setUserId(Integer userId) {
        this.userId = userId;
    }

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
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

    @Override
    public String toString() {
        return "WorkoutLog{" +
                "id=" + id +
                ", userId=" + userId +
                ", date=" + date +
                ", exerciseName='" + exerciseName + '\'' +
                ", sets=" + sets +
                ", reps=" + reps +
                ", weight=" + weight +
                '}';
    }
}
