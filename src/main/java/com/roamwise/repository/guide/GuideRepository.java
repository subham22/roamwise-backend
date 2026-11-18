package com.roamwise.repository.guide;

import com.roamwise.entity.guide.Guide;
import com.roamwise.entity.guide.Status;
import com.roamwise.dto.guide.GuideLocation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface GuideRepository extends JpaRepository<Guide, Integer> {
    Optional<Guide> findBySlug(String slug);
    List<Guide> findByStatus(Status status);

    @Query("select g.id from Guide g where g.status = com.roamwise.entity.guide.Status.PUBLISHED and g.travelInfo is null")
    List<Integer> findPublishedIdsWithoutTravelInfo();

    @Query("""
            select new com.roamwise.dto.guide.GuideLocation(g.slug, g.title, g.destination, g.metaDescription,
                   g.heroImageUrl, avg(a.latitude), avg(a.longitude))
            from GuideActivity a join a.guideDay d join d.guide g
            where g.status = com.roamwise.entity.guide.Status.PUBLISHED and a.latitude is not null
            group by g.id, g.slug, g.title, g.destination, g.metaDescription, g.heroImageUrl""")
    List<GuideLocation> findPublishedLocations();
}
