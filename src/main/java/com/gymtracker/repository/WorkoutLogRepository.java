package com.gymtracker.repository;

import com.gymtracker.entity.WorkoutLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface WorkoutLogRepository extends JpaRepository<WorkoutLog, Long> {

    List<WorkoutLog> findByUserIdAndDateGreaterThanEqualOrderByDateDescIdDesc(Long userId, LocalDate from);
}
