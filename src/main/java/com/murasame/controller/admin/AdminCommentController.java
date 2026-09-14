package com.murasame.controller.admin;

import com.murasame.mapper.CommentMapper;
import com.murasame.security.OperationLog;
import com.murasame.security.RequirePermission;
import com.murasame.util.ReturnUtil;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/admin/comments")
public class AdminCommentController {

    private final CommentMapper commentMapper;

    public AdminCommentController(CommentMapper commentMapper) {
        this.commentMapper = commentMapper;
    }

    /** 全站评论分页列表（含博客标题、评论人） */
    @GetMapping
    @RequirePermission("comment:list")
    public Map<String, Object> list(@RequestParam(defaultValue = "1") int page,
                                    @RequestParam(defaultValue = "10") int pageSize) {
        int offset = (page - 1) * pageSize;
        return ReturnUtil.success(Map.of(
                "list", commentMapper.getAllCommentsPage(pageSize, offset),
                "total", commentMapper.countAllComments()
        ));
    }

    /**
     * 删除评论（简单版：只删这一条，不级联子评论）
     * 注意：若删除的是父评论，其子评论的 parent_cid 会指向不存在的评论，成为孤儿。
     * 严谨版（后续优化）需递归删除所有子评论。
     */
    @OperationLog(module = "评论管理", action = "删除评论")
    @DeleteMapping("/{id}")
    @RequirePermission("comment:delete")
    public Map<String, Object> delete(@PathVariable Long id) {
        if (commentMapper.getCommentById(id) == null) {
            return ReturnUtil.notFound("评论不存在");
        }
        commentMapper.deleteCommentById(id);
        return ReturnUtil.success("评论删除成功");
    }

    /** 置顶评论（全站） */
    @OperationLog(module = "评论管理", action = "置顶评论")
    @PostMapping("/{id}/pin")
    @RequirePermission("comment:pin")
    public Map<String, Object> pin(@PathVariable Long id) {
        if (commentMapper.getCommentById(id) == null) {
            return ReturnUtil.notFound("评论不存在");
        }
        commentMapper.updatePinned(id, 1);
        return ReturnUtil.success("评论已置顶");
    }

    /** 取消置顶评论 */
    @OperationLog(module = "评论管理", action = "取消置顶")
    @PostMapping("/{id}/unpin")
    @RequirePermission("comment:pin")
    public Map<String, Object> unpin(@PathVariable Long id) {
        if (commentMapper.getCommentById(id) == null) {
            return ReturnUtil.notFound("评论不存在");
        }
        commentMapper.updatePinned(id, 0);
        return ReturnUtil.success("已取消置顶");
    }
}
