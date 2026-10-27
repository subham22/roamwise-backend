package com.roamwise.repository;

import com.roamwise.entity.Activity;
import com.roamwise.entity.ActivityVote;
import com.roamwise.entity.Trip;
import com.roamwise.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ActivityVoteRepository extends JpaRepository<ActivityVote, Integer> {
    List<ActivityVote> findByActivityDayTrip(Trip trip);
    Optional<ActivityVote> findByActivityAndUser(Activity activity, User user);
}
