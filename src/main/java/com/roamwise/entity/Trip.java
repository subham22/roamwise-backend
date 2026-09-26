package com.roamwise.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.util.Date;
import java.util.List;

@Entity
@Getter
@Setter
public class Trip {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer tripId;

    @ManyToOne
    private User user;

    @Column(nullable = false)
    private String destination;

    @Column(nullable = false)
    private String origin;

    private LocalDate startDate;

    private LocalDate endDate;

    private Integer budget;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, mappedBy = "trip")
    private List<Day> days;

    private String shareToken;

    private String budgetStayPct;
    private String budgetFoodPct;
    private String budgetActivitiesPct;
    private String budgetTransportPct;
    private String travelModeSuggestion;

}
