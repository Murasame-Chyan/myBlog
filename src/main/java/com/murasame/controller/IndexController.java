package com.murasame.controller;

import com.murasame.domain.vo.BlogBriefVO;
import com.murasame.domain.vo.CommentVO;
import com.murasame.entity.Tag;
import com.murasame.service.BlogService;
import com.murasame.service.CommentService;
import com.murasame.service.IndexService;
import com.murasame.service.TagService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Controller
@io.swagger.v3.oas.annotations.tags.Tag(name="主页接口", description = "主页相关操作")
public class IndexController {
	@Resource
	IndexService indexService;
	@Resource
	BlogService blogService;
	@Resource
	CommentService commentService;
	@Resource
	TagService tagService;

	@GetMapping({"/", "/index"})
	public String index(Model model,
	                    @RequestParam(required = false) String keyword,
	                    @RequestParam(required = false) String dateFrom,
	                    @RequestParam(required = false) String dateTo,
	                    @RequestParam(required = false) String sortBy,
	                    @RequestParam(defaultValue = "1") int page,
	                    @RequestParam(defaultValue = "5") int pageSize) {

		// 有任一搜索条件即走搜索分支，否则走默认全量分页（避免不必要的全文检索开销）
		boolean isSearch = (keyword != null && !keyword.isBlank())
				|| dateFrom != null || dateTo != null || sortBy != null;

		List<BlogBriefVO> blogBrief;
		long totalBlogs;
		int totalPages;

		if (isSearch) {
			LocalDateTime from = parseDate(dateFrom, true);
			LocalDateTime to = parseDate(dateTo, false);
			blogBrief = blogService.searchBlogs(keyword, from, to, sortBy, page, pageSize);
			totalBlogs = blogService.countSearchBlogs(keyword, from, to);
		} else {
			blogBrief = indexService.getBlogsByPage(page, pageSize);
			totalBlogs = indexService.getTotalBlogCount();
		}
		totalPages = (int) Math.ceil((double) totalBlogs / pageSize);

		List<CommentVO> recentComments = commentService.getRecentComments(5);
		List<Tag> allTags = tagService.getAllTags();
		List<BlogBriefVO> hotBlogs = indexService.getHotBlogs();

		model.addAttribute("blogBrief", blogBrief);
		model.addAttribute("currentPage", page);
		model.addAttribute("pageSize", pageSize);
		model.addAttribute("totalBlogs", totalBlogs);
		model.addAttribute("totalPages", totalPages);
		model.addAttribute("recentComments", recentComments);
		model.addAttribute("allTags", allTags);
		model.addAttribute("selectedTagId", null);
		model.addAttribute("hotBlogs", hotBlogs);
		model.addAttribute("keyword", keyword != null ? keyword : "");
		model.addAttribute("dateFrom", dateFrom != null ? dateFrom : "");
		model.addAttribute("dateTo", dateTo != null ? dateTo : "");
		model.addAttribute("sortBy", sortBy != null ? sortBy : "");
		return "index";
	}

	// startOfDay=true 返回当天00:00:00（起始），false 返回23:59:59.999（结束），用于日期范围筛选
	private LocalDateTime parseDate(String dateStr, boolean startOfDay) {
		if (dateStr == null || dateStr.isBlank()) return null;
		LocalDate date = LocalDate.parse(dateStr, DateTimeFormatter.ofPattern("yyyy-MM-dd"));
		return startOfDay ? date.atStartOfDay() : date.atTime(LocalTime.MAX);
	}

	// 调试端点，用于快速验证博客内容序列化是否正常
	@ResponseBody
	@GetMapping("/test")
	public String test() {
		Long id = 1L;
		return blogService.getBlogById_toString(id);
	}
}
