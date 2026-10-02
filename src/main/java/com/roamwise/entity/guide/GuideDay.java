package com.roamwise.entity.guide;


import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@Setter
public class GuideDay {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne
    @JoinColumn(name = "guide_id", nullable = false)
    private Guide guide;

    private String dayLabel;
    private Integer dayNo;

    @OneToMany(mappedBy = "guideDay", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<GuideActivity> activities = new ArrayList<>();
}
