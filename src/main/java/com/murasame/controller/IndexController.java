package com.murasame.controller;

import com.murasame.domain.vo.BlogBriefVO;
import com.murasame.service.BlogService;
import com.murasame.service.IndexService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.List;

@Controller
@Tag(name="主页接口", description = "主页相关操作")
public class IndexController {
	// 资源区
	@Resource
	IndexService indexService;
	@Resource
	BlogService blogService;

	// 博客主页
	@GetMapping({"/", "/index"})
	public String index(Model model) {
		List<BlogBriefVO> blogBrief = indexService.getRecent5BlogsBrief();
		model.addAttribute("blogBrief", blogBrief);
		return "index";
	}

	// test
	@ResponseBody
	@GetMapping("/test")
	public String test() {
		Long id = 1L;
		return blogService.getBlogById_toString(id);
	}
}
