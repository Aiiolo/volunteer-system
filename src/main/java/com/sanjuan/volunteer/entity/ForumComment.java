package com.sanjuan.volunteer.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "forum_comment")
public class ForumComment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id", nullable = false)
    private ForumPost post;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "author_id", nullable = false)
    private SysUser author;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(name = "create_time", nullable = false, updatable = false)
    private LocalDateTime createTime;

    @PrePersist
    void prePersist() {
        createTime = LocalDateTime.now();
    }
}
