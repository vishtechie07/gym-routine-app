package com.gymtracker.repository;

import com.gymtracker.entity.MealLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MealLogRepository extends JpaRepository<MealLog, Long> {
    // Basic CRUD operations are provided by JpaRepository
}
