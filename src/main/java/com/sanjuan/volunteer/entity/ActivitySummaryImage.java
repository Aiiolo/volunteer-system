package com.sanjuan.volunteer.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "activity_summary_image")
public class ActivitySummaryImage {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "summary_id", nullable = false)
    private ActivitySummary summary;

    @Column(nullable = false, length = 512)
    private String url;

    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder = 0;
}
