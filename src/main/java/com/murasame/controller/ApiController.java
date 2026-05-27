package com.murasame.controller;

import com.murasame.domain.vo.BlogBriefVO;
import com.murasame.domain.vo.CommentVO;
import com.murasame.domain.vo.UserProfileVO;
import com.murasame.entity.Blogs;
import com.murasame.entity.BlogsBin;
import com.murasame.entity.Tag;
import com.murasame.entity.Users;
import com.murasame.service.*;
import com.murasame.util.AuthHelper;
import com.murasame.util.BlogHtmlUtil;
import com.murasame.util.ReturnUtil;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RequestMapping("/api")
@RestController
@io.swagger.v3.oas.annotations.tags.Tag(name = "API接口", description = "供 Vue 前端使用的 JSON API")
public class ApiController {

    @Resource private IndexService indexService;
    @Resource private BlogService blogService;
    @Resource private CommentService commentService;
    @Resource private TagService tagService;
    @Resource private UserService userService;
    @Resource private LikesService likesService;
    @Resource private AuthHelper authHelper;

    // ===== 博客列表 =====
    @GetMapping("/blogs")
    public Map<String, Object> listBlogs(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String dateFrom,
            @RequestParam(required = false) String dateTo,
            @RequestParam(required = false) String sortBy,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "5") int pageSize) {

        boolean isSearch = (keyword != null && !keyword.isBlank())
                || dateFrom != null || dateTo != null || sortBy != null;

        List<BlogBriefVO> blogBrief;
        long totalBlogs;

        if (isSearch) {
            LocalDateTime from = parseDate(dateFrom, true);
            LocalDateTime to = parseDate(dateTo, false);
            blogBrief = blogService.searchBlogs(keyword, from, to, sortBy, page, pageSize);
            totalBlogs = blogService.countSearchBlogs(keyword, from, to);
        } else {
            blogBrief = indexService.getBlogsByPage(page, pageSize);
            totalBlogs = indexService.getTotalBlogCount();
        }

        int totalPages = (int) Math.ceil((double) totalBlogs / pageSize);
        return ReturnUtil.success(Map.of(
                "blogs", blogBrief,
                "currentPage", page,
                "pageSize", pageSize,
                "totalBlogs", totalBlogs,
                "totalPages", totalPages
        ));
    }

    // ===== 标签列表 =====
    @GetMapping("/tags")
    public Map<String, Object> listTags() {
        List<Tag> tags = tagService.getAllTags();
        return ReturnUtil.success(tags);
    }

    // ===== 热门文章 =====
    @GetMapping("/hot-blogs")
    public Map<String, Object> hotBlogs() {
        List<BlogBriefVO> hot = indexService.getHotBlogs();
        return ReturnUtil.success(hot);
    }

    // ===== 最新评论 =====
    @GetMapping("/recent-comments")
    public Map<String, Object> recentComments() {
        List<CommentVO> comments = commentService.getRecentComments(5);
        return ReturnUtil.success(comments);
    }

    // ===== 博客详情（content 为 HTML 渲染后，rawContent 为 Markdown 原文供编辑） =====
    @GetMapping("/blogs/{id}")
    public Map<String, Object> blogDetail(@PathVariable Long id, HttpServletRequest request) {
        Blogs blog = blogService.getBlogById(id);
        if (blog == null) return ReturnUtil.error("博客不存在");

        String rawContent = blog.getContent();
        blog.setContent(BlogHtmlUtil.toHtml(rawContent));

        List<CommentVO> comments = commentService.getCommentTree(id);
        int commentCount = commentService.getCommentCountByBlogId(id);
        Users authorUser = userService.getUserById(blog.getU_id());
        String authorName = authorUser != null ? authorUser.getNickname() : "未知用户";
        String authorAvatar = authorUser != null ? authorUser.getAvatar() : null;

        boolean liked = false;
        Users currentUser = authHelper.getCurrentUser(request);
        if (currentUser != null) {
            liked = likesService.isLiked(currentUser.getId(), id);
        }

        return ReturnUtil.success(Map.of(
                "blog", blog,
                "rawContent", rawContent,
                "comments", comments,
                "commentCount", commentCount,
                "authorName", authorName,
                "authorAvatar", authorAvatar,
                "liked", liked
        ));
    }

    // ===== 用户主页数据 =====
    @GetMapping("/user/profile/{id}")
    public Map<String, Object> userProfile(@PathVariable Long id) {
        Users user = userService.getUserById(id);
        if (user == null) return ReturnUtil.error("用户不存在");

        int level = userService.calculateLevel(
                user.getExp() != null ? user.getExp() : 0);
        List<BlogBriefVO> articles = blogService.getUserBlogs(id, "", "newest", 1, 100);
        List<BlogBriefVO> likedBlogs = likesService.getLikedBlogs(id, 50);

        return ReturnUtil.success(Map.of(
                "user", UserProfileVO.from(user),
                "level", level,
                "articles", articles != null ? articles : List.of(),
                "likedBlogs", likedBlogs
        ));
    }

    // ===== 当前用户资料（编辑弹窗） =====
    @GetMapping("/user/profile-data")
    public Map<String, Object> currentUserProfileData(HttpServletRequest request) {
        Users currentUser = authHelper.getCurrentUser(request);
        if (currentUser == null) return ReturnUtil.unauthorized("请先登录");
        Users user = userService.getUserById(currentUser.getId());
        if (user == null) return ReturnUtil.error("用户不存在");
        return ReturnUtil.success(user);
    }

    // ===== 归档列表 =====
    @GetMapping("/archives")
    public Map<String, Object> archives(HttpServletRequest request) {
        Users currentUser = authHelper.getCurrentUser(request);
        if (currentUser == null) return ReturnUtil.unauthorized("请先登录");
        List<BlogsBin> bins = blogService.getUserBins(currentUser.getId());
        return ReturnUtil.success(bins != null ? bins : List.of());
    }

    // ===== 归档详情 =====
    @GetMapping("/archives/{id}")
    public Map<String, Object> archiveDetail(@PathVariable Long id, HttpServletRequest request) {
        Users currentUser = authHelper.getCurrentUser(request);
        if (currentUser == null) return ReturnUtil.unauthorized("请先登录");

        Blogs bin = blogService.getBlogFromBinById(id);
        if (bin == null) return ReturnUtil.error("归档文章不存在");

        if (!bin.getU_id().equals(currentUser.getId())) {
            return ReturnUtil.unauthorized("无权查看此归档文章");
        }

        String rawContent = bin.getContent();
        bin.setContent(BlogHtmlUtil.toHtml(rawContent));

        Users authorUser = userService.getUserById(bin.getU_id());
        String authorName = authorUser != null ? authorUser.getNickname() : "未知用户";
        String authorAvatar = authorUser != null ? authorUser.getAvatar() : null;

        return ReturnUtil.success(Map.of(
                "archive", bin,
                "rawContent", rawContent,
                "authorName", authorName,
                "authorAvatar", authorAvatar
        ));
    }

    private LocalDateTime parseDate(String dateStr, boolean startOfDay) {
        if (dateStr == null || dateStr.isBlank()) return null;
        LocalDate date = LocalDate.parse(dateStr, DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        return startOfDay ? date.atStartOfDay() : date.atTime(LocalTime.MAX);
    }
}
