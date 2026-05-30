package com.sanjuan.volunteer.controller;

import com.sanjuan.volunteer.common.ApiResponse;
import com.sanjuan.volunteer.dto.Dtos;
import com.sanjuan.volunteer.entity.Enums;
import com.sanjuan.volunteer.exception.BusinessException;
import com.sanjuan.volunteer.security.CurrentUser;
import com.sanjuan.volunteer.service.FileUploadService;
import com.sanjuan.volunteer.service.ForumService;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/v1/forum")
public class ForumApiController {

    private final ForumService forumService;
    private final FileUploadService fileUploadService;

    public ForumApiController(ForumService forumService, FileUploadService fileUploadService) {
        this.forumService = forumService;
        this.fileUploadService = fileUploadService;
    }

    @GetMapping("/posts/manage")
    public ApiResponse<List<Dtos.ForumPostSummary>> listManagePosts(
            @AuthenticationPrincipal CurrentUser user,
            @RequestParam String filter) {
        if (user == null || user.id() == null) {
            return ApiResponse.fail("未登录或登录已过期");
        }
        Enums.Role role = Enums.Role.valueOf(user.role());
        return ApiResponse.ok(forumService.listManagePosts(user.id(), role, filter));
    }

    @GetMapping("/posts")
    public ApiResponse<List<Dtos.ForumPostSummary>> listPosts(@AuthenticationPrincipal CurrentUser user) {
        if (user == null || user.id() == null) {
            return ApiResponse.fail("未登录或登录已过期");
        }
        Enums.Role role = Enums.Role.valueOf(user.role());
        return ApiResponse.ok(forumService.listPosts(user.id(), role));
    }

    @GetMapping("/posts/{id}")
    public ApiResponse<Dtos.ForumPostDetail> getPost(@AuthenticationPrincipal CurrentUser user,
                                                     @PathVariable Long id) {
        if (user == null || user.id() == null) {
            return ApiResponse.fail("未登录或登录已过期");
        }
        return ApiResponse.ok(forumService.getPost(id, user.id(), Enums.Role.valueOf(user.role())));
    }

    @PostMapping("/posts")
    public ApiResponse<Dtos.ForumPostDetail> createPost(@AuthenticationPrincipal CurrentUser user,
                                                        @Valid @RequestBody Dtos.ForumPostCreateRequest request) {
        if (user == null || user.id() == null) {
            return ApiResponse.fail("未登录或登录已过期");
        }
        return ApiResponse.ok(forumService.createPost(user.id(), Enums.Role.valueOf(user.role()), request));
    }

    @PostMapping("/notices")
    public ApiResponse<Dtos.ForumPostDetail> createNotice(@AuthenticationPrincipal CurrentUser user,
                                                          @Valid @RequestBody Dtos.ForumNoticeCreateRequest request) {
        if (user == null || user.id() == null) {
            return ApiResponse.fail("未登录或登录已过期");
        }
        Enums.Role role = Enums.Role.valueOf(user.role());
        if (role != Enums.Role.ADMIN && role != Enums.Role.ORGANIZER) {
            return ApiResponse.fail("仅管理员与活动负责人可发布活动通知");
        }
        return ApiResponse.ok(forumService.createNotice(user.id(), role, request));
    }

    @PutMapping("/posts/{id}")
    public ApiResponse<Dtos.ForumPostDetail> updatePost(@AuthenticationPrincipal CurrentUser user,
                                                        @PathVariable Long id,
                                                        @Valid @RequestBody Dtos.ForumPostUpdateRequest request) {
        if (user == null || user.id() == null) {
            return ApiResponse.fail("未登录或登录已过期");
        }
        return ApiResponse.ok(forumService.updatePost(id, user.id(), Enums.Role.valueOf(user.role()), request));
    }

    @GetMapping("/linkable-activities")
    public ApiResponse<List<Dtos.ForumActivityOption>> linkableActivities(
            @AuthenticationPrincipal CurrentUser user,
            @RequestParam(defaultValue = "post") String purpose) {
        if (user == null || user.id() == null) {
            return ApiResponse.fail("未登录或登录已过期");
        }
        Enums.Role role = parseRole(user.role());
        if (role == null) {
            return ApiResponse.ok(List.of());
        }
        try {
            return ApiResponse.ok(forumService.linkableActivities(user.id(), role, purpose));
        } catch (BusinessException ex) {
            return ApiResponse.ok(List.of());
        }
    }

    private static Enums.Role parseRole(String role) {
        if (role == null || role.isBlank()) {
            return null;
        }
        try {
            return Enums.Role.valueOf(role.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    @PostMapping("/posts/{id}/like")
    public ApiResponse<Dtos.ForumPostDetail> toggleLike(@AuthenticationPrincipal CurrentUser user,
                                                         @PathVariable Long id) {
        if (user == null || user.id() == null) {
            return ApiResponse.fail("未登录或登录已过期");
        }
        return ApiResponse.ok(forumService.toggleLike(id, user.id(), Enums.Role.valueOf(user.role())));
    }

    @PostMapping("/posts/{id}/comments")
    public ApiResponse<Dtos.ForumPostDetail> addComment(@AuthenticationPrincipal CurrentUser user,
                                                        @PathVariable Long id,
                                                        @Valid @RequestBody Dtos.ForumCommentRequest request) {
        if (user == null || user.id() == null) {
            return ApiResponse.fail("未登录或登录已过期");
        }
        return ApiResponse.ok(forumService.addComment(id, user.id(), Enums.Role.valueOf(user.role()), request));
    }

    @PutMapping("/posts/{id}/manage")
    public ApiResponse<Dtos.ForumPostSummary> managePost(@AuthenticationPrincipal CurrentUser user,
                                                         @PathVariable Long id,
                                                         @RequestBody Dtos.ForumAdminUpdateRequest request) {
        if (user == null || user.id() == null) {
            return ApiResponse.fail("未登录或登录已过期");
        }
        return ApiResponse.ok(forumService.managePost(id, user.id(), Enums.Role.valueOf(user.role()), request));
    }

    @DeleteMapping("/posts/{id}")
    public ApiResponse<Void> deletePost(@AuthenticationPrincipal CurrentUser user, @PathVariable Long id) {
        if (user == null || user.id() == null) {
            return ApiResponse.fail("未登录或登录已过期");
        }
        forumService.deletePost(id, user.id(), Enums.Role.valueOf(user.role()));
        return ApiResponse.ok("已移入回收站", null);
    }

    @PostMapping("/posts/{id}/restore")
    public ApiResponse<Dtos.ForumPostSummary> restorePost(@AuthenticationPrincipal CurrentUser user,
                                                          @PathVariable Long id) {
        if (user == null || user.id() == null) {
            return ApiResponse.fail("未登录或登录已过期");
        }
        return ApiResponse.ok(forumService.restorePost(id, user.id(), Enums.Role.valueOf(user.role())));
    }

    @DeleteMapping("/posts/{id}/permanent")
    public ApiResponse<Void> purgePost(@AuthenticationPrincipal CurrentUser user, @PathVariable Long id) {
        if (user == null || user.id() == null) {
            return ApiResponse.fail("未登录或登录已过期");
        }
        forumService.purgePost(id, user.id(), Enums.Role.valueOf(user.role()));
        return ApiResponse.ok("已永久删除", null);
    }

    @PostMapping("/upload")
    public ApiResponse<Dtos.UploadResponse> upload(@RequestParam("file") MultipartFile file) {
        return ApiResponse.ok(new Dtos.UploadResponse(fileUploadService.store(file)));
    }
}
