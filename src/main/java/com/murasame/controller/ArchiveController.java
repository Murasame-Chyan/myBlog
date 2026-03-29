package com.murasame.controller;

import com.murasame.domain.vo.BlogBriefVO;
import com.murasame.entity.BlogsBin;
import com.murasame.entity.Tag;
import com.murasame.service.ArchiveService;
import com.murasame.service.TagService;
import com.murasame.service.UserService;
import jakarta.annotation.Resource;
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
	                       @RequestParam(defaultValue = "5") int pageSize) {
		List<BlogBriefVO> archiveBrief = archiveService.getArchivesByPage(page, pageSize);
		long totalArchives = archiveService.getTotalArchiveCount();
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
	public String readArchive(@PathVariable Long id, Model model) {
		BlogsBin archive = archiveService.getArchiveById(id);
		String authorName = userService.getNicknameById(archive.getU_id());
		List<Tag> allTags = tagService.getAllTags();
		model.addAttribute("archive", archive);
		model.addAttribute("allTags", allTags);
		model.addAttribute("authorName", authorName);
		return "readArchive";
	}

	@GetMapping("/archives/tag/{id}")
	public String getArchivesByTag(@PathVariable Integer id, Model model) {
		List<BlogBriefVO> archiveBriefList = archiveService.getArchivesByTagId(id);
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
