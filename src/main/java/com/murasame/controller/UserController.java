package com.murasame.controller;

import com.murasame.domain.vo.BlogBriefVO;
import com.murasame.domain.vo.CommentVO;
import com.murasame.entity.Users;
import com.murasame.service.BlogService;
import com.murasame.service.CommentService;
import com.murasame.service.CosUploadService;
import com.murasame.service.LikesService;
import com.murasame.service.UserService;
import com.murasame.util.ReturnUtil;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RequestMapping("/user")
@Controller
@Validated
@Tag(name="用户接口", description = "用户评论、头像等相关接口")
public class UserController {
	@Resource
	private CommentService commentService;

	@Resource
	private CosUploadService cosUploadService;

	@Resource
	private BlogService blogService;

	@Resource
	private UserService userService;
	@Resource
	private LikesService likesService;

	@ResponseBody
	@PostMapping("/comment/add")
	public Map<String, Object> addComment(
			@RequestParam Long blogId,
			@RequestParam(required = false) Long parentCid,
			@Size(max = 65535, message = "评论内容过长")
			@RequestParam String content,
			HttpSession session) {
		Users currentUser = (Users) session.getAttribute("currentUser");
		if (currentUser == null) {
			return ReturnUtil.unauthorized("请先登录");
		}
		Long authorId = currentUser.getId();
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

	@ResponseBody
	@PostMapping("/avatar/upload")
	public Map<String, Object> uploadAvatar(
			@RequestParam("file") MultipartFile file,
			HttpSession session) {

		Users currentUser = (Users) session.getAttribute("currentUser");
		if (currentUser == null) {
			return ReturnUtil.error("请先登录");
		}
		Long userId = currentUser.getId();

		try {
			String avatarUrl = cosUploadService.uploadAvatar(file, userId);
			int result = userService.updateAvatar(userId, avatarUrl);
			if (result > 0) {
				currentUser.setAvatar(avatarUrl);
				session.setAttribute("currentUser", currentUser);
				return ReturnUtil.success("头像上传成功", avatarUrl);
			} else {
				return ReturnUtil.error("数据库更新失败");
			}
		} catch (Exception e) {
			return ReturnUtil.error("头像上传失败: " + e.getMessage());
		}
	}

	@GetMapping("/profile")
	public String profile(@RequestParam(required = false) Long id,
	                      HttpSession session, Model model) {
		Users currentUser = (Users) session.getAttribute("currentUser");
		if (currentUser == null) {
			return "redirect:/";
		}
		Long profileUserId = (id != null) ? id : currentUser.getId();
		Users profileUser = userService.getUserById(profileUserId);
		if (profileUser == null) {
			return "redirect:/";
		}
		model.addAttribute("profileUser", profileUser);
		model.addAttribute("isOwner", currentUser.getId().equals(profileUserId));
		model.addAttribute("githubUsername", profileUser.getGithubUsername());
		return "profile";
	}

	@ResponseBody
	@PostMapping("/profile/update")
	public Map<String, Object> updateProfile(
			@NotBlank(message = "昵称不能为空")
			@Size(max = 32, message = "昵称不能超过32个字符")
			@RequestParam String nickname,
			@Size(max = 255, message = "简介不能超过255个字符")
			@RequestParam(required = false) String intro,
			@NotBlank(message = "邮箱不能为空")
			@Email(message = "邮箱格式不正确")
			@Size(max = 255, message = "邮箱不能超过255个字符")
			@RequestParam String email,
			@RequestParam Integer gender,
			@Size(max = 255, message = "GitHub用户名不能超过255个字符")
			@RequestParam(required = false) String githubUsername,
			HttpSession session) {
		Users currentUser = (Users) session.getAttribute("currentUser");
		if (currentUser == null) {
			return ReturnUtil.unauthorized("请先登录");
		}
		try {
			Users user = new Users();
			user.setId(currentUser.getId());
			user.setNickname(nickname);
			user.setIntro(intro);
			user.setEmail(email);
			user.setGender(gender);
			user.setGithubUsername(githubUsername);
			Users updated = userService.updateProfile(user);
			session.setAttribute("currentUser", updated);
			return ReturnUtil.success("保存成功");
		} catch (IllegalArgumentException e) {
			return ReturnUtil.error(e.getMessage());
		}
	}

	@ResponseBody
	@GetMapping("/profile/blogs")
	public Map<String, Object> getUserBlogs(
			@RequestParam Long userId,
			@RequestParam(required = false) String keyword,
			@RequestParam(defaultValue = "newest") String sortBy,
			@RequestParam(defaultValue = "1") int page,
			@RequestParam(defaultValue = "10") int pageSize) {
		List<BlogBriefVO> list = blogService.getUserBlogs(userId,
				keyword != null && keyword.isBlank() ? null : keyword,
				sortBy, page, pageSize);
		long total = blogService.countUserBlogs(userId,
				keyword != null && keyword.isBlank() ? null : keyword);
		Map<String, Object> data = new HashMap<>();
		data.put("list", list);
		data.put("total", total);
		data.put("page", page);
		data.put("pageSize", pageSize);
		return ReturnUtil.success("获取成功", data);
	}

	@ResponseBody
	@GetMapping("/profile/likes")
	public Map<String, Object> getLikedBlogs(@RequestParam Long userId, HttpSession session) {
		Users currentUser = (Users) session.getAttribute("currentUser");
		if (currentUser == null || !currentUser.getId().equals(userId)) {
			return ReturnUtil.success("获取成功", Collections.emptyList());
		}
		List<BlogBriefVO> likedBlogs = likesService.getLikedBlogs(userId, 10);
		return ReturnUtil.success("获取成功", likedBlogs);
	}
}
