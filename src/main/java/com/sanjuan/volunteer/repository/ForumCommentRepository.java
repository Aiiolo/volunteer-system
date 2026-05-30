package com.sanjuan.volunteer.repository;

import com.sanjuan.volunteer.entity.ForumComment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ForumCommentRepository extends JpaRepository<ForumComment, Long> {
    List<ForumComment> findByPostIdOrderByCreateTimeAsc(Long postId);
    long countByPostId(Long postId);
}
