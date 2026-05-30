package com.sanjuan.volunteer.repository;

import com.sanjuan.volunteer.entity.Enums;
import com.sanjuan.volunteer.entity.ForumPost;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface ForumPostRepository extends JpaRepository<ForumPost, Long> {

    @Query("""
            SELECT p FROM ForumPost p
            WHERE p.deleted = false
            AND (:admin = true OR p.hidden = false OR p.author.id = :viewerId)
            ORDER BY p.topped DESC, p.createTime DESC
            """)
    List<ForumPost> findAllForViewer(Long viewerId, boolean admin);

    @Query("""
            SELECT p FROM ForumPost p
            WHERE p.deleted = false AND p.author.id = :userId
            ORDER BY p.topped DESC, p.createTime DESC
            """)
    List<ForumPost> findPublishedByAuthor(Long userId);

    @Query("""
            SELECT p FROM ForumPost p
            WHERE p.deleted = true AND p.author.id = :userId
            ORDER BY p.deletedTime DESC, p.createTime DESC
            """)
    List<ForumPost> findDeletedByAuthor(Long userId);

    @Query("""
            SELECT p FROM ForumPost p
            WHERE p.deleted = false AND p.hidden = true AND p.author.id = :userId
            ORDER BY p.createTime DESC
            """)
    List<ForumPost> findHiddenByAuthor(Long userId);

    @Query("""
            SELECT p FROM ForumPost p
            WHERE p.deleted = false AND p.postType = :postType
            ORDER BY p.topped DESC, p.createTime DESC
            """)
    List<ForumPost> findAllActiveByType(Enums.PostType postType);

    @Query("""
            SELECT p FROM ForumPost p
            WHERE p.deleted = false AND p.hidden = true
            ORDER BY p.createTime DESC
            """)
    List<ForumPost> findAllHidden();

    @Query("""
            SELECT p FROM ForumPost p
            WHERE p.deleted = true
            ORDER BY p.deletedTime DESC, p.createTime DESC
            """)
    List<ForumPost> findAllDeleted();
}
