package com.murasame.controller;

import com.murasame.domain.vo.BlogBriefVO;
import com.murasame.domain.vo.CommentVO;
import com.murasame.entity.BlogsBin;
import com.murasame.service.ArchiveService;
import com.murasame.service.CommentService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@Controller
@Tag(name="归档接口", description = "归档相关操作")
public class ArchiveController {
	@Resource
	ArchiveService archiveService;
	@Resource
	CommentService commentService;

	@GetMapping("/archives")
	public String archives(Model model,
	                       @RequestParam(defaultValue = "1") int page,
	                       @RequestParam(defaultValue = "5") int pageSize) {
		List<BlogBriefVO> archiveBrief = archiveService.getArchivesByPage(page, pageSize);
		long totalArchives = archiveService.getTotalArchiveCount();
		int totalPages = (int) Math.ceil((double) totalArchives / pageSize);
		
		model.addAttribute("archiveBrief", archiveBrief);
		model.addAttribute("currentPage", page);
		model.addAttribute("pageSize", pageSize);
		model.addAttribute("totalArchives", totalArchives);
		model.addAttribute("totalPages", totalPages);
		return "archives";
	}

	@GetMapping("/archives/read/{id}")
	public String readArchive(@PathVariable Long id, Model model) {
		BlogsBin archive = archiveService.getArchiveById(id);
		if (archive == null) {
			String errorInf = "您访问的归档不存在！";
			model.addAttribute("errorInf", errorInf);
			return "error";
		}
		model.addAttribute("archive", archive);
		return "readArchive";
	}
}
