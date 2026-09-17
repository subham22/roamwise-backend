package com.roamwise.repository;


import com.roamwise.entity.Activity;
import com.roamwise.entity.Day;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ActivityRepository extends JpaRepository<Activity, Integer> {
    List<Activity> findByDay(Day day);
}
