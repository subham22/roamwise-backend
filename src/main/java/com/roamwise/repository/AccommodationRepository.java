package com.roamwise.repository;

import com.roamwise.entity.Accommodation;
import com.roamwise.entity.Trip;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AccommodationRepository extends JpaRepository<Accommodation, Integer> {
    List<Accommodation> findByTrip(Trip trip);
}