package com.murasame.controller.admin;

import com.murasame.mapper.AnnouncementMapper;
import com.murasame.mapper.BlogMapper;
import com.murasame.mapper.CommentMapper;
import com.murasame.mapper.UserMapper;
import com.murasame.security.RequirePermission;
import com.murasame.util.ReturnUtil;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 管理端仪表盘统计接口
 * 提供总数统计与近 30 天趋势数据
 */
@RestController
@RequestMapping("/api/admin/dashboard")
public class AdminDashboardController {

    private final UserMapper userMapper;
    private final BlogMapper blogMapper;
    private final CommentMapper commentMapper;
    private final AnnouncementMapper announcementMapper;

    public AdminDashboardController(UserMapper userMapper,
                                    BlogMapper blogMapper,
                                    CommentMapper commentMapper,
                                    AnnouncementMapper announcementMapper) {
        this.userMapper = userMapper;
        this.blogMapper = blogMapper;
        this.commentMapper = commentMapper;
        this.announcementMapper = announcementMapper;
    }

    /** 仪表盘统计数据：总数 + 近 30 天博客/评论趋势 */
    @GetMapping("/stats")
    @RequirePermission("dashboard:view")
    public Map<String, Object> stats() {
        return ReturnUtil.success(Map.of(
                "userCount", userMapper.countAll(null),
                "blogCount", blogMapper.countSearchBlogs(null, null, null),
                "commentCount", commentMapper.countAllComments(),
                "announcementCount", announcementMapper.count(null),
                "blogTrend", blogMapper.getBlogTrend(30),
                "commentTrend", commentMapper.getCommentTrend(30)
        ));
    }
}
