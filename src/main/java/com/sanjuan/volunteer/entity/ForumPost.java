package com.sanjuan.volunteer.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Entity
@Table(name = "forum_post")
public class ForumPost {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "author_id", nullable = false)
    private SysUser author;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "activity_id")
    private Activity activity;

    @Column(name = "view_count", nullable = false)
    private Integer viewCount = 0;

    @Column(name = "like_count", nullable = false)
    private Integer likeCount = 0;

    @Column(nullable = false)
    private Boolean hidden = false;

    @Column(nullable = false)
    private Boolean topped = false;

    @Enumerated(EnumType.STRING)
    @Column(name = "post_type", nullable = false, length = 20)
    private Enums.PostType postType = Enums.PostType.POST;

    @Column(nullable = false)
    private Boolean deleted = false;

    @Column(name = "deleted_time")
    private LocalDateTime deletedTime;

    @Column(name = "create_time", nullable = false, updatable = false)
    private LocalDateTime createTime;

    @OneToMany(mappedBy = "post", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("sortOrder ASC")
    private List<ForumPostImage> images = new ArrayList<>();

    @PrePersist
    void prePersist() {
        createTime = LocalDateTime.now();
    }
}
