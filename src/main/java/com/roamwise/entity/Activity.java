package com.roamwise.entity;

import com.roamwise.dto.review.Review;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalTime;

@Getter
@Setter
@Entity
public class Activity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    private String name;

    @ManyToOne
    private Day day;

    private LocalTime startTime;

    private LocalTime endTime;

    private Integer sequence;

    private String notes;

    private Double latitude;
    private Double longitude;
    @Column(columnDefinition = "TEXT")
    private String photoReference;

    private Boolean isHiddenGem;

    @Column(columnDefinition = "TEXT")
    private String reviewSnippet;
}
