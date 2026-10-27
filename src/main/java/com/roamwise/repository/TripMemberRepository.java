package com.roamwise.repository;

import com.roamwise.entity.Trip;
import com.roamwise.entity.TripMember;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TripMemberRepository extends JpaRepository<TripMember, Integer> {
    List<TripMember> findByTripOrderByCreatedAtAsc(Trip trip);
    Optional<TripMember> findByTripAndEmail(Trip trip, String email);
    List<TripMember> findByEmail(String email);
    long countByTrip(Trip trip);
}
