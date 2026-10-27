package com.roamwise.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.time.LocalDateTime;

/** Someone invited to a trip. Matched by email, so invites work before the person signs up. */
@Entity
@Getter
@Setter
@Table(uniqueConstraints = @UniqueConstraint(columnNames = {"trip_trip_id", "email"}))
public class TripMember {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(optional = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn(name = "trip_trip_id")
    private Trip trip;

    @Column(nullable = false)
    private String email; // stored lower-case

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TripRole role; // EDITOR or VIEWER

    private LocalDateTime createdAt;
}
