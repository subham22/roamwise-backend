package com.roamwise.repository.guide;

import com.roamwise.entity.guide.Guide;
import com.roamwise.entity.guide.Status;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface GuideRepository extends JpaRepository<Guide, Integer> {
    Optional<Guide> findBySlug(String slug);
    List<Guide> findByStatus(Status status);
}
