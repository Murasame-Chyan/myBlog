package com.murasame.controller;

import com.murasame.domain.vo.BlogBriefVO;
import com.murasame.entity.Blogs;
import com.murasame.service.IndexService;
import jakarta.annotation.Resource;
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

	// 博客主页
	@GetMapping({"/", "/index"})
	public String index(Model model) {
		List<BlogBriefVO> blogBrief = indexService.getRecent5BlogsBrief();
		model.addAttribute("blogBrief", blogBrief);
		return "index";
	}

	// 点击跳转文章正文 RESTful跟随文章id
	@GetMapping("/blogs/{id}")
	public String readBlog(@PathVariable BigInteger id, Model model) {
		Blogs blog = indexService.getBlogById(id);
		if (blog == null) {
			String errorInf = "您访问的博客不存在！";
			model.addAttribute("errorInf", errorInf);
			return "error";
		}
		model.addAttribute("blog", blog);
		return "readBlog";
	}

	// test
	@ResponseBody
	@GetMapping("/test")
	public String test() {
		BigInteger id = new BigInteger("1");
		return indexService.getBlogById_toString(id);
	}
}
