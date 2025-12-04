package com.murasame.controller;

import com.murasame.domain.vo.BlogBriefVO;
import com.murasame.entity.Blogs;
import com.murasame.service.BlogService;
import com.murasame.service.IndexService;
import jakarta.annotation.Resource;
import jakarta.annotation.Resources;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.ResponseBody;

import java.math.BigInteger;
import java.util.List;

@Controller
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
