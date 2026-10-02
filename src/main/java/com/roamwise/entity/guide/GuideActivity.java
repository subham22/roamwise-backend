package com.roamwise.entity.guide;


import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.time.LocalTime;

@Entity
@Getter
@Setter
public class GuideActivity {


    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne
    @JoinColumn(name = "guide_day_id", nullable = false)
    private GuideDay guideDay ;

    private  String name;

    private Integer sequence;

    private LocalTime startTime;

    private LocalTime endTime;

    private String notes;

    private Double latitude;
    private Double longitude;
    @Column(columnDefinition = "TEXT")
    private String photoReference;

    private Boolean isHiddenGem;

    @Column(columnDefinition = "TEXT")
    private String reviewSnippet;


}
