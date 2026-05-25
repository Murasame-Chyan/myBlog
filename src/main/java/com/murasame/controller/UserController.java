package com.murasame.controller;

import com.murasame.domain.vo.BlogBriefVO;
import com.murasame.domain.vo.CommentVO;
import com.murasame.entity.Users;
import com.murasame.service.BlogService;
import com.murasame.service.CommentService;
import com.murasame.service.CosUploadService;
import com.murasame.service.FollowService;
import com.murasame.service.LikesService;
import com.murasame.service.UserService;
import com.murasame.util.ReturnUtil;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Collections;
import com.murasame.util.AuthHelper;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RequestMapping("/user")
@Controller
@Validated
@Tag(name="用户接口", description = "用户评论、头像等相关接口")
public class UserController {
	@Resource
	private AuthHelper authHelper;

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

	@Resource
	private FollowService followService;

	@ResponseBody
	@PostMapping("/comment/add")
	public Map<String, Object> addComment(
			@RequestParam("blogId") Long blogId,
			@RequestParam(value = "parentId", required = false) Long parentCid,
			@Size(max = 65535, message = "评论内容过长")
			@RequestParam String content,
			HttpServletRequest request) {
		Users currentUser = authHelper.getCurrentUser(request);
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

	// 头像上传三步流程：1. COS云端上传 → 2. 数据库URL更新 → 3. Session同步刷新
	@ResponseBody
	@PostMapping("/avatar/upload")
	public Map<String, Object> uploadAvatar(
			@RequestParam("file") MultipartFile file,
			HttpServletRequest request) {

		Users currentUser = authHelper.getCurrentUser(request);
		if (currentUser == null) {
			return ReturnUtil.error("请先登录");
		}
		Long userId = currentUser.getId();

		try {
			String avatarUrl = cosUploadService.uploadAvatar(file, userId);
			int result = userService.updateAvatar(userId, avatarUrl);
			if (result > 0) {
				currentUser.setAvatar(avatarUrl);
				request.getSession().setAttribute("currentUser", currentUser);
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
	                      HttpServletRequest request, Model model) {
		Users currentUser = authHelper.getCurrentUser(request);
		// 未登录且未指定用户id时无法查看"我的主页"，重定向到首页
		if (currentUser == null && id == null) {
			return "redirect:/";
		}
		Long profileUserId = (id != null) ? id : currentUser.getId();
		Users profileUser = userService.getUserById(profileUserId);
		if (profileUser == null) {
			return "redirect:/";
		}
		boolean isOwner = currentUser != null && currentUser.getId().equals(profileUserId);
		model.addAttribute("profileUser", profileUser);
		model.addAttribute("isOwner", isOwner);
		model.addAttribute("isLoggedIn", currentUser != null);
		model.addAttribute("githubUsername", profileUser.getGithubUsername());
		// 关注状态（仅登录用户查看他人主页时查询）
		if (currentUser != null && !isOwner) {
			model.addAttribute("isFollowing", followService.isFollowing(currentUser.getId(), profileUserId));
		}
		model.addAttribute("followerCount", followService.countFollowers(profileUserId));
		model.addAttribute("followingCount", followService.countFollowing(profileUserId));
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
			@RequestParam(required = false) String githubToken,
			HttpServletRequest request) {
		Users currentUser = authHelper.getCurrentUser(request);
		if (currentUser == null) {
			return ReturnUtil.unauthorized("请先登录");
		}
		// 将分散的请求参数组装为Users对象，便于Service层统一处理
		try {
			Users user = new Users();
			user.setId(currentUser.getId());
			user.setNickname(nickname);
			user.setIntro(intro);
			user.setEmail(email);
			user.setGender(gender);
			user.setGithubUsername(githubUsername);
			if (githubToken != null && !githubToken.isBlank()) {
				user.setGithubToken(githubToken.trim());
			}
			Users updated = userService.updateProfile(user);
			// 更新 session 时清除 githubToken（不解出到前端 session）
			updated.setGithubToken(null);
			request.getSession().setAttribute("currentUser", updated);
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
	public Map<String, Object> getLikedBlogs(@RequestParam Long userId, HttpServletRequest request) {
		Users currentUser = authHelper.getCurrentUser(request);
		if (currentUser == null || !currentUser.getId().equals(userId)) {
			return ReturnUtil.success("获取成功", Collections.emptyList());
		}
		List<BlogBriefVO> likedBlogs = likesService.getLikedBlogs(userId, 10);
		return ReturnUtil.success("获取成功", likedBlogs);
	}

	@ResponseBody
	@PostMapping("/follow/{followeeId}")
	public Map<String, Object> follow(@PathVariable Long followeeId, HttpServletRequest request) {
		Users currentUser = authHelper.getCurrentUser(request);
		if (currentUser == null) {
			return ReturnUtil.unauthorized("请先登录");
		}
		if (currentUser.getId().equals(followeeId)) {
			return ReturnUtil.error("不能关注自己");
		}
		followService.follow(currentUser.getId(), followeeId);
		return ReturnUtil.success("关注成功");
	}

	@ResponseBody
	@PostMapping("/unfollow/{followeeId}")
	public Map<String, Object> unfollow(@PathVariable Long followeeId, HttpServletRequest request) {
		Users currentUser = authHelper.getCurrentUser(request);
		if (currentUser == null) {
			return ReturnUtil.unauthorized("请先登录");
		}
		followService.unfollow(currentUser.getId(), followeeId);
		return ReturnUtil.success("已取消关注");
	}

	@ResponseBody
	@GetMapping("/follow/status")
	public Map<String, Object> followStatus(@RequestParam Long followeeId, HttpServletRequest request) {
		Users currentUser = authHelper.getCurrentUser(request);
		if (currentUser == null) {
			return ReturnUtil.success("获取成功", false);
		}
		boolean following = followService.isFollowing(currentUser.getId(), followeeId);
		return ReturnUtil.success("获取成功", following);
	}

	@ResponseBody
	@GetMapping("/follow/counts")
	public Map<String, Object> followCounts(@RequestParam Long userId) {
		Map<String, Integer> data = new HashMap<>();
		data.put("followerCount", followService.countFollowers(userId));
		data.put("followingCount", followService.countFollowing(userId));
		return ReturnUtil.success("获取成功", data);
	}

	// 粉丝列表页
	@GetMapping("/{userId}/followers")
	public String followersPage(@PathVariable Long userId, Model model, HttpServletRequest request) {
		Users profileUser = userService.getUserById(userId);
		if (profileUser == null) return "redirect:/";
		Users currentUser = authHelper.getCurrentUser(request);
		model.addAttribute("profileUser", profileUser);
		model.addAttribute("isLoggedIn", currentUser != null);
		model.addAttribute("listType", "followers");
		model.addAttribute("pageTitle", "粉丝");
		return "follow-list";
	}

	// 关注列表页
	@GetMapping("/{userId}/following")
	public String followingPage(@PathVariable Long userId, Model model, HttpServletRequest request) {
		Users profileUser = userService.getUserById(userId);
		if (profileUser == null) return "redirect:/";
		Users currentUser = authHelper.getCurrentUser(request);
		model.addAttribute("profileUser", profileUser);
		model.addAttribute("isLoggedIn", currentUser != null);
		model.addAttribute("listType", "following");
		model.addAttribute("pageTitle", "关注");
		return "follow-list";
	}

	// 粉丝列表 JSON（附带当前用户对各粉丝的回关状态）
	@ResponseBody
	@GetMapping("/follow/followers")
	public Map<String, Object> listFollowers(@RequestParam Long userId,
	                                          @RequestParam(defaultValue = "1") int page,
	                                          @RequestParam(defaultValue = "20") int pageSize,
	                                          HttpServletRequest request) {
		Users currentUser = authHelper.getCurrentUser(request);
		List<Users> list = followService.getFollowers(userId, page, pageSize);
		int total = followService.countFollowers(userId);
		List<Map<String, Object>> items = list.stream().map(u -> {
			Map<String, Object> m = new HashMap<>();
			m.put("user", u);
			m.put("isFollowing", currentUser != null && followService.isFollowing(currentUser.getId(), u.getId()));
			return m;
		}).toList();
		Map<String, Object> data = new HashMap<>();
		data.put("list", items);
		data.put("total", total);
		data.put("page", page);
		data.put("pageSize", pageSize);
		return ReturnUtil.success("获取成功", data);
	}

	// 关注列表 JSON（附带当前用户对各用户的关注状态）
	@ResponseBody
	@GetMapping("/follow/following")
	public Map<String, Object> listFollowing(@RequestParam Long userId,
	                                          @RequestParam(defaultValue = "1") int page,
	                                          @RequestParam(defaultValue = "20") int pageSize,
	                                          HttpServletRequest request) {
		Users currentUser = authHelper.getCurrentUser(request);
		List<Users> list = followService.getFollowing(userId, page, pageSize);
		int total = followService.countFollowing(userId);
		List<Map<String, Object>> items = list.stream().map(u -> {
			Map<String, Object> m = new HashMap<>();
			m.put("user", u);
			m.put("isFollowing", currentUser != null && followService.isFollowing(currentUser.getId(), u.getId()));
			return m;
		}).toList();
		Map<String, Object> data = new HashMap<>();
		data.put("list", items);
		data.put("total", total);
		data.put("page", page);
		data.put("pageSize", pageSize);
		return ReturnUtil.success("获取成功", data);
	}

	// 移除粉丝（被我方删除关注关系：对方不再关注我）
	@ResponseBody
	@PostMapping("/follow/remove-follower/{followerId}")
	public Map<String, Object> removeFollower(@PathVariable Long followerId, HttpServletRequest request) {
		Users currentUser = authHelper.getCurrentUser(request);
		if (currentUser == null) return ReturnUtil.unauthorized("请先登录");
		followService.unfollow(followerId, currentUser.getId());
		return ReturnUtil.success("已移除");
	}
}
