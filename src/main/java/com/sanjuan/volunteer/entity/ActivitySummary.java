package com.sanjuan.volunteer.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;
import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "activity_summary")
public class ActivitySummary {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "activity_id", nullable = false, unique = true)
    private Activity activity;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organizer_id", nullable = false)
    private SysUser organizer;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(columnDefinition = "TEXT")
    private String feedback;

    @Column(name = "submit_time", nullable = false)
    private LocalDateTime submitTime;

    @Column(name = "update_time")
    private LocalDateTime updateTime;

    @Column(name = "admin_viewed", nullable = false)
    private Boolean adminViewed = false;

    @OneToMany(mappedBy = "summary", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("sortOrder ASC")
    private List<ActivitySummaryImage> images = new ArrayList<>();

    @PrePersist
    void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        submitTime = now;
        updateTime = now;
        if (adminViewed == null) {
            adminViewed = false;
        }
    }

    @PreUpdate
    void preUpdate() {
        updateTime = LocalDateTime.now();
    }
}
