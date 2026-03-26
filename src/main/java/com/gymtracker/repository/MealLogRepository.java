package com.gymtracker.repository;

import com.gymtracker.entity.MealLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface MealLogRepository extends JpaRepository<MealLog, Long> {

    List<MealLog> findByUserIdAndDateGreaterThanEqualOrderByDateDescIdDesc(Long userId, LocalDate from);

    List<MealLog> findByUserIdAndDateOrderByMealTimeAscIdAsc(Long userId, LocalDate date);

    Optional<MealLog> findByIdAndUserId(Long id, Long userId);
}
