package com.roamwise.repository;

import com.roamwise.entity.Day;
import com.roamwise.entity.Trip;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DayRepository extends JpaRepository<Day, Integer> {
    List<Day> findByTrip(Trip trip);
}
