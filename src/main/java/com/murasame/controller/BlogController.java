package com.murasame.controller;


import com.murasame.domain.dto.TagWrapper;
import com.murasame.domain.vo.BlogBriefVO;
import com.murasame.domain.vo.CommentVO;
import com.murasame.entity.Blogs;
import com.murasame.entity.Tag;
import com.murasame.service.BlogService;
import com.murasame.service.CommentService;
import com.murasame.service.LikesService;
import com.murasame.service.TagService;
import com.murasame.service.UserService;
import com.murasame.util.BlogHtmlUtil;
import com.murasame.util.ReturnUtil;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import com.murasame.util.AuthHelper;

@RequestMapping("/blogs")
@Controller
@Validated
@io.swagger.v3.oas.annotations.tags.Tag(name="博客接口", description = "博客相关CRUD")
public class BlogController {
	@Resource
	private AuthHelper authHelper;


	private BlogService blogService;
	@Resource
	private CommentService commentService;
	@Resource
	private TagService tagService;
	@Resource
	private UserService userService;
	@Resource
	private LikesService likesService;

	// From index 点击跳转文章正文 RESTful跟随文章id
	@GetMapping("read/{id}")
	public String readBlog(@PathVariable Long id, Model model) {
		Blogs blog = blogService.getBlogById(id);
		if (blog == null) {
			String errorInf = "您访问的博客不存在！";
			model.addAttribute("errorInf", errorInf);
			return "error";
		}

		// 将 Markdown 转为安全的 HTML
		String htmlContent = BlogHtmlUtil.toHtml(blog.getContent());
		blog.setContent(htmlContent);

		List<CommentVO> comments = commentService.getCommentTree(id);
		List<Tag> allTags = tagService.getAllTags();
		int commentCount = commentService.getCommentCountByBlogId(id);
		com.murasame.entity.Users authorUser = userService.getUserById(blog.getU_id());
		String authorName = authorUser != null ? authorUser.getNickname() : "未知用户";
		String authorAvatar = authorUser != null ? authorUser.getAvatar() : null;

		model.addAttribute("blog", blog);
		model.addAttribute("comments", comments);
		model.addAttribute("allTags", allTags);
		model.addAttribute("commentCount", commentCount);
		model.addAttribute("authorName", authorName);
		model.addAttribute("authorAvatar", authorAvatar);
		return "readBlog";
	}

	// From index 点击发布跳转编辑页
	@GetMapping("/write")
	public String writeBlog(Model model) {
		List<Tag> allTags = tagService.getAllTags();
		model.addAttribute("allTags", allTags);
		return "writeBlog";
	}

	@ResponseBody
	@PostMapping("/publish")
	public Map<String, Object> publishBlog(
			@NotBlank(message = "标题不能为空")
			@Size(max = 255, message = "标题不能超过255个字符")
			@RequestParam String title,
			@NotBlank(message = "内容不能为空")
			@RequestParam String content,
			@RequestParam(value = "tagIds", required = false) String tagIds,
			@RequestParam(value = "newTagNames", required = false) String newTagNames,
			HttpServletRequest request) {
		com.murasame.entity.Users currentUser = authHelper.getCurrentUser(request);
		if (currentUser == null) {
			return ReturnUtil.unauthorized();
		}
		Long authorId = currentUser.getId();
		List<Integer> tagList = resolveTagIds(tagIds, newTagNames);
		if (tagList.size() > 10) {
			return ReturnUtil.error("标签最多选择10个");
		}
		TagWrapper tagWrapper = new TagWrapper();
		tagWrapper.setTagList(tagList);
		int newId = blogService.publishBlogWithTags(authorId, title, content, tagWrapper);
		if (newId > 0) {
			return ReturnUtil.custom(200, "发布成功", newId);
		} else {
			return ReturnUtil.error("发布失败");
		}
	}

	// From readBlog 点击修改文章按钮
	@GetMapping("/edit/{id}")
	public String editBlog(@PathVariable Long id, Model model, HttpServletRequest request) {
		com.murasame.entity.Users currentUser = authHelper.getCurrentUser(request);
		if (currentUser == null) {
			return "redirect:/";
		}
		Blogs blog = blogService.getBlogById(id);
		if (blog == null || !blog.getU_id().equals(currentUser.getId())) {
			return "redirect:/";
		}
		List<Tag> allTags = tagService.getAllTags();
		model.addAttribute("blog", blog);
		model.addAttribute("allTags", allTags);
		return "writeBlog";
	}

	@ResponseBody
	@PostMapping("/update")
	public Map<String, Object> updateBlog(
			@NotBlank(message = "标题不能为空")
			@Size(max = 255, message = "标题不能超过255个字符")
			@RequestParam String title,
			@NotBlank(message = "内容不能为空")
			@RequestParam String content,
			@RequestParam Long id,
			@RequestParam(value = "tagIds", required = false) String tagIds,
			@RequestParam(value = "newTagNames", required = false) String newTagNames,
			HttpServletRequest request) {
		com.murasame.entity.Users currentUser = authHelper.getCurrentUser(request);
		if (currentUser == null) {
			return ReturnUtil.unauthorized();
		}
		Blogs existing = blogService.getBlogById(id);
		if (existing == null) {
			return ReturnUtil.error("博客不存在");
		}
		if (!existing.getU_id().equals(currentUser.getId())) {
			return ReturnUtil.unauthorized("无权修改此文章");
		}
		List<Integer> tagList = resolveTagIds(tagIds, newTagNames);
		if (tagList.size() > 10) {
			return ReturnUtil.error("标签最多选择10个");
		}
		TagWrapper tagWrapper = new TagWrapper();
		tagWrapper.setTagList(tagList);
		int result = blogService.updateBlogWithTags(id, title, content, tagWrapper);
		if (result > 0) {
			return ReturnUtil.success("成功更新");
		} else {
			return ReturnUtil.error("更新失败");
		}
	}

	// From readBlog 删除文章（软删除移入回收箱）
	@ResponseBody
	@PostMapping("/delete/{id}")
	public Map<String, Object> deleteBlog(@PathVariable Long id, HttpServletRequest request) {
		com.murasame.entity.Users currentUser = authHelper.getCurrentUser(request);
		if (currentUser == null) {
			return ReturnUtil.unauthorized();
		}
		Blogs existing = blogService.getBlogById(id);
		if (existing == null) {
			return ReturnUtil.error("博客不存在");
		}
		if (!existing.getU_id().equals(currentUser.getId())) {
			return ReturnUtil.unauthorized("无权删除此文章");
		}
		int dropStatus = blogService.dropBlogToBin(id);
		return dropStatus == 1 ? ReturnUtil.success("已移入回收箱") : ReturnUtil.error("博客不存在");
	}

	// 恢复博客：校验登录态与所有权，仅作者本人可恢复
	@ResponseBody
	@PostMapping("/recover/{id}")
	public Map<String, Object> recoverBlog(@PathVariable Long id, HttpServletRequest request) {
		com.murasame.entity.Users currentUser = authHelper.getCurrentUser(request);
		if (currentUser == null) {
			return ReturnUtil.unauthorized();
		}
		Blogs binEntry = blogService.getBlogFromBinById(id);
		if (binEntry == null) {
			return ReturnUtil.error("回收站中不存在此博客");
		}
		if (!binEntry.getU_id().equals(currentUser.getId())) {
			return ReturnUtil.unauthorized("无权恢复此文章");
		}
		int recoverStatus = blogService.recoverBlogFromBin(id);
		return recoverStatus == 1 ? ReturnUtil.success("博客已重新发布") : ReturnUtil.error("博客不存在");
	}

	@GetMapping("/tag/{id}")
	public String getBlogsByTag(@PathVariable Integer id, Model model) {
		List<Blogs> blogs = blogService.getBlogsByTagId(id);
		List<BlogBriefVO> blogBriefList = new ArrayList<>();
		List<Tag> allTags = tagService.getAllTags();
		for (Blogs blog : blogs) {
			BlogBriefVO vo = new BlogBriefVO();
			vo.setId(blog.getId());
			vo.setU_id(blog.getU_id());
			vo.setTitle(blog.getTitle());
			vo.setBrief(BlogHtmlUtil.extractBrief(blog.getContent()));
			vo.setCreated_at(blog.getCreated_at());
			vo.setUpdated_at(blog.getUpdated_at());
			vo.setAuthor(userService.getNicknameById(blog.getU_id()));
			vo.setT_id(blog.getT_id());
			vo.setRead_count(blog.getRead_count() != null ? blog.getRead_count() : 0L);
			vo.setLike_count(blog.getLike_count() != null ? blog.getLike_count() : 0L);
			vo.setComment_count(commentService.getCommentCountByBlogId(blog.getId()));
			blogBriefList.add(vo);
		}
		model.addAttribute("blogBrief", blogBriefList);
		model.addAttribute("totalBlogs", blogBriefList.size());
		model.addAttribute("currentPage", 1);
		model.addAttribute("totalPages", 1);
		model.addAttribute("allTags", allTags);
		model.addAttribute("selectedTagId", id);
		return "index";
	}

	// 点赞博客
	@ResponseBody
	@PostMapping("/like/{id}")
	public Map<String, Object> likeBlog(@PathVariable Long id, HttpServletRequest request) {
		com.murasame.entity.Users currentUser = authHelper.getCurrentUser(request);
		if (currentUser == null) {
			return ReturnUtil.unauthorized("请先登录");
		}
		if (likesService.isLiked(currentUser.getId(), id)) {
			return ReturnUtil.error("已经点赞过了");
		}
		likesService.like(currentUser.getId(), id);
		int result = blogService.incrementLikeCount(id);
		if (result > 0) {
			Blogs blog = blogService.getBlogById(id);
			return ReturnUtil.success("点赞成功", blog.getLike_count());
		} else {
			return ReturnUtil.error("点赞失败");
		}
	}

	// 取消点赞
	@ResponseBody
	@PostMapping("/unlike/{id}")
	public Map<String, Object> unlikeBlog(@PathVariable Long id, HttpServletRequest request) {
		com.murasame.entity.Users currentUser = authHelper.getCurrentUser(request);
		if (currentUser == null) {
			return ReturnUtil.unauthorized("请先登录");
		}
		if (!likesService.isLiked(currentUser.getId(), id)) {
			return ReturnUtil.error("尚未点赞");
		}
		likesService.unlike(currentUser.getId(), id);
		int result = blogService.decrementLikeCount(id);
		if (result > 0) {
			Blogs blog = blogService.getBlogById(id);
			return ReturnUtil.success("取消点赞成功", blog.getLike_count());
		} else {
			return ReturnUtil.error("取消点赞失败");
		}
	}

	// 检查是否已点赞
	@ResponseBody
	@GetMapping("/isLiked/{id}")
	public Map<String, Object> isLiked(@PathVariable Long id, HttpServletRequest request) {
		com.murasame.entity.Users currentUser = authHelper.getCurrentUser(request);
		if (currentUser == null) {
			return ReturnUtil.success("未点赞", false);
		}
		boolean liked = likesService.isLiked(currentUser.getId(), id);
		return ReturnUtil.success(liked ? "已点赞" : "未点赞", liked);
	}

	// 增加阅读量（前端延迟调用）
	@ResponseBody
	@PostMapping("/incrementRead/{id}")
	public Map<String, Object> incrementReadCount(@PathVariable Long id) {
		int result = blogService.incrementReadCount(id);
		if (result > 0) {
			return ReturnUtil.success("阅读量已更新");
		} else {
			return ReturnUtil.error("更新失败");
		}
	}

	// 将已有标签ID和新标签名合并为标签ID列表，不存在的标签自动创建
	private List<Integer> resolveTagIds(String tagIds, String newTagNames) {
		List<Integer> tagList = new ArrayList<>();
		if (tagIds != null && !tagIds.isEmpty()) {
			for (String s : tagIds.split(",")) {
				String trimmed = s.trim();
				if (!trimmed.isEmpty()) {
					tagList.add(Integer.parseInt(trimmed));
				}
			}
		}
		if (newTagNames != null && !newTagNames.isEmpty()) {
			for (String name : newTagNames.split(",")) {
				String trimmed = name.trim();
				if (!trimmed.isEmpty() && trimmed.length() <= 255) {
					Tag tag = tagService.createOrGetTag(trimmed);
					if (!tagList.contains(tag.getId())) {
						tagList.add(tag.getId());
					}
				}
			}
		}
		return tagList;
	}
}
