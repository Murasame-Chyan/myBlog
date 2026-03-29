package com.murasame.controller;

import com.murasame.domain.vo.CommentVO;
import com.murasame.service.CommentService;
import com.murasame.util.ReturnUtil;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RequestMapping("/user")
@Controller
@Tag(name="用户评论接口", description = "用户评论相关接口")
public class UserController {
	@Resource
	private CommentService commentService;

	@ResponseBody
	@PostMapping("/comment/add")
	public Map<String, Object> addComment(
			@RequestParam Long blogId,
			@RequestParam(required = false) Long parentCid,
			@RequestParam Long authorId,
			@RequestParam String content) {
		int newId = commentService.addComment(blogId, parentCid, authorId, content);
		if (newId > 0) {
			return ReturnUtil.custom(200, "评论成功", newId);
		} else {
			return ReturnUtil.error("评论失败");
		}
	}

	@ResponseBody
	@GetMapping("/comment/list/{blogId}")
	public Map<String, Object> getCommentsByBlogId(@PathVariable Long blogId) {
		List<CommentVO> comments = commentService.getCommentTree(blogId);
		return ReturnUtil.success("获取成功", comments);
	}

	@ResponseBody
	@GetMapping("/comment/recent")
	public Map<String, Object> getRecentComments(@RequestParam(defaultValue = "5") int limit) {
		List<CommentVO> comments = commentService.getRecentComments(limit);
		return ReturnUtil.success("获取成功", comments);
	}
}
