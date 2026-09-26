package com.roamwise.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
public class ReplanProposal {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne
    private Trip trip;

    @Column(columnDefinition = "TEXT")
    private String proposedJson; // the raw AI response, not yet applied

    @Column(columnDefinition = "TEXT")
    private String diffSummary; // serialized diff list, e.g. joined with newlines

    private LocalDateTime createdAt;

    @Column(columnDefinition = "TEXT")
    private String placesJson;
}