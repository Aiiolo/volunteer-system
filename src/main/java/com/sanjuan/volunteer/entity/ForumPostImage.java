package com.sanjuan.volunteer.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "forum_post_image")
public class ForumPostImage {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id", nullable = false)
    private ForumPost post;

    @Column(nullable = false, length = 512)
    private String url;

    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder = 0;
}
