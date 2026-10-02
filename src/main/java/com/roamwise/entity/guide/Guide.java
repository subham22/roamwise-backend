package com.roamwise.entity.guide;


import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@Setter
public class Guide {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Integer id;

  @Column(unique = true)
  private String slug;
  private String title;
  private String origin;
  private String destination;
  private String metaDescription;

  @Column(columnDefinition = "TEXT")
  private String heroImageUrl;

  @Enumerated(EnumType.STRING)
  private Status status;

  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "guide", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<GuideDay> days = new ArrayList<>();
}

