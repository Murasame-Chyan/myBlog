package com.murasame.controller;

import com.murasame.domain.vo.BlogBriefVO;
import com.murasame.entity.BlogsBin;
import com.murasame.entity.Tag;
import com.murasame.entity.Users;
import com.murasame.service.ArchiveService;
import com.murasame.service.TagService;
import com.murasame.service.UserService;
import com.murasame.util.BlogHtmlUtil;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@Controller
@io.swagger.v3.oas.annotations.tags.Tag(name="归档接口", description = "归档相关操作")
public class ArchiveController {
	@Resource
	ArchiveService archiveService;
	@Resource
	TagService tagService;

	@Resource
	UserService userService;

	@GetMapping("/archives")
	public String archives(Model model,
	                       @RequestParam(defaultValue = "1") int page,
	                       @RequestParam(defaultValue = "5") int pageSize,
	                       HttpSession session) {
		Users currentUser = (Users) session.getAttribute("currentUser");
		if (currentUser == null) {
			return "redirect:/";
		}
		Long uId = currentUser.getId();

		List<BlogBriefVO> archiveBrief = archiveService.getArchivesByPage(page, pageSize, uId);
		long totalArchives = archiveService.getTotalArchiveCount(uId);
		int totalPages = (int) Math.ceil((double) totalArchives / pageSize);
		List<Tag> allTags = tagService.getAllTags();

		model.addAttribute("archiveBrief", archiveBrief);
		model.addAttribute("currentPage", page);
		model.addAttribute("pageSize", pageSize);
		model.addAttribute("totalArchives", totalArchives);
		model.addAttribute("totalPages", totalPages);
		model.addAttribute("allTags", allTags);
		return "archives";
	}

	@GetMapping("/archives/read/{id}")
	public String readArchive(@PathVariable Long id, Model model, HttpSession session) {
		Users currentUser = (Users) session.getAttribute("currentUser");
		if (currentUser == null) {
			return "redirect:/";
		}
		BlogsBin archive = archiveService.getArchiveById(id);
		if (archive == null || archive.getU_id() == null || !archive.getU_id().equals(currentUser.getId())) {
			return "redirect:/";
		}
		// 将 Markdown 转为安全的 HTML，与 readBlog 保持一致
		archive.setContent(BlogHtmlUtil.toHtml(archive.getContent()));
		Users authorUser = userService.getUserById(archive.getU_id());
		String authorName = authorUser != null ? authorUser.getNickname() : "未知用户";
		String authorAvatar = authorUser != null ? authorUser.getAvatar() : null;
		List<Tag> allTags = tagService.getAllTags();
		model.addAttribute("archive", archive);
		model.addAttribute("allTags", allTags);
		model.addAttribute("authorName", authorName);
		model.addAttribute("authorAvatar", authorAvatar);
		return "readArchive";
	}

	@GetMapping("/archives/tag/{id}")
	public String getArchivesByTag(@PathVariable Integer id, Model model,
	                                HttpSession session) {
		Users currentUser = (Users) session.getAttribute("currentUser");
		if (currentUser == null) {
			return "redirect:/";
		}
		Long uId = currentUser.getId();

		List<BlogBriefVO> archiveBriefList = archiveService.getArchivesByTagId(id, uId);
		List<Tag> allTags = tagService.getAllTags();
		model.addAttribute("archiveBrief", archiveBriefList);
		model.addAttribute("totalArchives", archiveBriefList.size());
		model.addAttribute("currentPage", 1);
		model.addAttribute("totalPages", 1);
		model.addAttribute("allTags", allTags);
		model.addAttribute("selectedTagId", id);
		return "archives";
	}
}
