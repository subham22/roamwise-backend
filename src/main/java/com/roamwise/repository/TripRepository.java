package com.roamwise.repository;

import com.roamwise.entity.Trip;
import com.roamwise.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TripRepository extends JpaRepository<Trip, Integer> {

    List<Trip> findByUser(User user);

    Optional<Trip> findByShareToken(String sharedToken);
}
