package com.murasame.controller;


import com.murasame.domain.dto.TagWrapper;
import com.murasame.domain.vo.BlogBriefVO;
import com.murasame.domain.vo.CommentVO;
import com.murasame.entity.Blogs;
import com.murasame.entity.Tag;
import com.murasame.service.BlogService;
import com.murasame.service.CommentService;
import com.murasame.service.TagService;
import com.murasame.service.UserService;
import com.murasame.util.BlogHtmlUtil;
import com.murasame.util.ReturnUtil;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RequestMapping("/blogs")
@Controller
@io.swagger.v3.oas.annotations.tags.Tag(name="博客接口", description = "博客相关CRUD")
public class BlogController {
	@Resource
	private BlogService blogService;
	@Resource
	private CommentService commentService;
	@Resource
	private TagService tagService;
	@Resource
	private UserService userService;

	// From index 点击跳转文章正文 RESTful跟随文章id
	@GetMapping("read/{id}")
	public String readBlog(@PathVariable Long id, Model model) {
		Blogs blog = blogService.getBlogById(id);
		if (blog == null) {
			String errorInf = "您访问的博客不存在！";
			model.addAttribute("errorInf", errorInf);
			return "error";
		}
		
		List<CommentVO> comments = commentService.getCommentTree(id);
		List<Tag> allTags = tagService.getAllTags();
		int commentCount = commentService.getCommentCountByBlogId(id);
		String authorName = userService.getNicknameById(blog.getU_id());
		
		model.addAttribute("blog", blog);
		model.addAttribute("comments", comments);
		model.addAttribute("allTags", allTags);
		model.addAttribute("commentCount", commentCount);
		model.addAttribute("authorName", authorName);
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
			@RequestParam String title,
			@RequestParam String content,
			@RequestParam(value = "authorId", defaultValue = "1") Long authorId,
			@RequestParam(value = "tagIds", required = false) String tagIds) {
		int newId;
		if (tagIds != null && !tagIds.isEmpty()) {
			TagWrapper tagWrapper = new TagWrapper();
			List<Integer> tagList = new ArrayList<>();
			for (String tagId : tagIds.split(",")) {
				if (!tagId.trim().isEmpty()) {
					tagList.add(Integer.parseInt(tagId.trim()));
				}
			}
			tagWrapper.setTagList(tagList);
			newId = blogService.publishBlogWithTags(authorId, title, content, tagWrapper);
		} else {
			newId = blogService.publishBlog(authorId, title, content);
		}
		if (newId > 0) {
			return ReturnUtil.custom(200, "发布成功", newId);
		} else {
			return ReturnUtil.error("发布失败");
		}
	}

	// From readBlog 点击修改文章按钮
	@GetMapping("/edit/{id}")
	public String editBlog(@PathVariable Long id, Model model) {
		Blogs blog = blogService.getBlogById(id);
		if (blog != null) {
			List<Tag> allTags = tagService.getAllTags();
			model.addAttribute("blog", blog);
			model.addAttribute("allTags", allTags);
			return "writeBlog";
		}
		return "redirect:/read/{id}";
	}
	@ResponseBody
	@PostMapping("/update")
	public Map<String, Object> updateBlog(
			@RequestParam String title,
			@RequestParam String content,
			@RequestParam Long id,
			@RequestParam(value = "tagIds", required = false) String tagIds,
			Model model) {
		if (!model.containsAttribute("id"))
			model.addAttribute("id", id);
		int result;
		if (tagIds != null && !tagIds.isEmpty()) {
			TagWrapper tagWrapper = new TagWrapper();
			List<Integer> tagList = new ArrayList<>();
			for (String tagId : tagIds.split(",")) {
				if (!tagId.trim().isEmpty()) {
					tagList.add(Integer.parseInt(tagId.trim()));
				}
			}
			tagWrapper.setTagList(tagList);
			result = blogService.updateBlogWithTags(id, title, content, tagWrapper);
		} else {
			result = blogService.updateBlog(id, title, content);
		}
		if (result > 0) {
			return ReturnUtil.success("成功更新");
		} else {
			return ReturnUtil.error("更新失败");
		}
	}

	// From noWhere（testing in swagger）
	@ResponseBody
	@PostMapping("/delete/{id}")
	public String deleteBlog(@PathVariable Long id){
		int dropStatus = blogService.dropBlogToBin(id);
		return dropStatus == 1 ? "已移入回收箱。" : "博客不存在！";
	}
	@ResponseBody
	@PostMapping("/deleteAll")
	public String deleteBlog(){
		int dropStatus = blogService.moveAllBlogsToBin();
		return dropStatus != 0 ? "已全体移入回收箱。" : "全体移除失败！";
	}
	@ResponseBody
	@PostMapping("/recover/{id}")
	public String recoverBlog(@PathVariable Long id){
		int recoverStatus = blogService.recoverBlogFromBin(id);
		return recoverStatus == 1 ? "博客已重新发布！" : "博客不存在！";
	}

	@GetMapping("/tag/{id}")
	public String getBlogsByTag(@PathVariable Integer id, Model model) {
		List<Blogs> blogs = blogService.getBlogsByTagId(id);
		List<BlogBriefVO> blogBriefList = new ArrayList<>();
		List<Tag> allTags = tagService.getAllTags();
		for (Blogs blog : blogs) {
			BlogBriefVO vo = new BlogBriefVO();
			vo.setId(blog.getId());
			vo.setTitle(blog.getTitle());
			vo.setBrief(BlogHtmlUtil.extractBrief(blog.getContent()));
			vo.setAuthor(blog.getU_id().toString());
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
	public Map<String, Object> likeBlog(@PathVariable Long id, @RequestParam(value = "userId", required = false, defaultValue = "1") Long userId) {
		if (userService.isBlogLiked(userId, id)) {
			return ReturnUtil.error("已经点赞过了");
		}
		
		com.murasame.service.impl.UserServiceImpl userServiceImpl = (com.murasame.service.impl.UserServiceImpl) userService;
		boolean addResult = userServiceImpl.addBlogToLiked(userId, id);
		if (!addResult) {
			return ReturnUtil.error("点赞失败");
		}
		
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
	public Map<String, Object> unlikeBlog(@PathVariable Long id, @RequestParam(value = "userId", required = false, defaultValue = "1") Long userId) {
		if (!userService.isBlogLiked(userId, id)) {
			return ReturnUtil.error("尚未点赞");
		}
		
		com.murasame.service.impl.UserServiceImpl userServiceImpl = (com.murasame.service.impl.UserServiceImpl) userService;
		boolean removeResult = userServiceImpl.removeBlogFromLiked(userId, id);
		if (!removeResult) {
			return ReturnUtil.error("取消点赞失败");
		}
		
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
	public Map<String, Object> isLiked(@PathVariable Long id, @RequestParam(value = "userId", required = false, defaultValue = "1") Long userId) {
		boolean liked = userService.isBlogLiked(userId, id);
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
}
