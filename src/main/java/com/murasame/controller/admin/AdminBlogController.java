package com.murasame.controller.admin;

import com.murasame.mapper.BlogMapper;
import com.murasame.security.OperationLog;
import com.murasame.security.RequirePermission;
import com.murasame.service.BlogService;
import com.murasame.util.ReturnUtil;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/admin/blogs")
public class AdminBlogController {

    private final BlogService blogService;
    private final BlogMapper blogMapper;

    public AdminBlogController(BlogService blogService, BlogMapper blogMapper) {
        this.blogService = blogService;
        this.blogMapper = blogMapper;
    }

    /**
     * 分页列出全站博客（含作者、标签、阅读量等，复用 searchBlogs）
     * keyword 为空则查全部
     */
    @GetMapping
    @RequirePermission("blog:list")
    public Map<String, Object> list(@RequestParam(defaultValue = "1") int page,
                                    @RequestParam(defaultValue = "10") int pageSize,
                                    @RequestParam(required = false) String keyword) {
        // searchBlogs 里 keyword 为 null 时 WHERE 条件不加过滤，等价于查全站
        return ReturnUtil.success(Map.of(
                "list", blogService.searchBlogs(keyword, null, null, "newest", page, pageSize),
                "total", blogService.countSearchBlogs(keyword, null, null)
        ));
    }

    /** 回收站列表（全站已下架的博客） */
    @GetMapping("/bin")
    @RequirePermission("blog:list")
    public Map<String, Object> binList(@RequestParam(defaultValue = "1") int page,
                                       @RequestParam(defaultValue = "10") int pageSize,
                                       @RequestParam(required = false) String keyword) {
        int offset = (page - 1) * pageSize;
        return ReturnUtil.success(Map.of(
                "list", blogMapper.findBinPage(keyword, pageSize, offset),
                "total", blogMapper.countBin(keyword)
        ));
    }

    /** 下架博客：移入回收站（复用 dropBlogToBin） */
    @OperationLog(module = "博客管理", action = "下架博客")
    @PostMapping("/{id}/offline")
    @RequirePermission("blog:offline")
    public Map<String, Object> offline(@PathVariable Long id) {
        if (blogService.getBlogById(id) == null) {
            return ReturnUtil.notFound("博客不存在或已下架");
        }
        blogService.dropBlogToBin(id);
        return ReturnUtil.success("博客已下架");
    }

    /** 恢复博客：从回收站移回（复用 recoverBlogFromBin） */
    @OperationLog(module = "博客管理", action = "恢复博客")
    @PostMapping("/{id}/recover")
    @RequirePermission("blog:offline")
    public Map<String, Object> recover(@PathVariable Long id) {
        if (blogService.getBlogFromBinById(id) == null) {
            return ReturnUtil.notFound("回收站中不存在该博客");
        }
        blogService.recoverBlogFromBin(id);
        return ReturnUtil.success("博客已恢复");
    }

    /** 永久删除：从回收站彻底删除（不可恢复） */
    @OperationLog(module = "博客管理", action = "永久删除博客")
    @DeleteMapping("/{id}")
    @RequirePermission("blog:delete")
    public Map<String, Object> delete(@PathVariable Long id) {
        // 只允许删除回收站里的博客，防止误删在线博客
        if (blogService.getBlogFromBinById(id) == null) {
            return ReturnUtil.error("请先下架博客再永久删除");
        }
        blogMapper.removeBlogFromBin(id);
        return ReturnUtil.success("博客已永久删除");
    }
}
