package com.gymtracker.repository;

import com.gymtracker.entity.WorkoutLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface WorkoutLogRepository extends JpaRepository<WorkoutLog, Long> {
    // Basic CRUD operations are provided by JpaRepository
}
