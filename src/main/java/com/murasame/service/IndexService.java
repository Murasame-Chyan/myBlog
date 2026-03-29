package com.murasame.service;

import com.murasame.domain.vo.BlogBriefVO;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public interface IndexService {
	List<BlogBriefVO> getRecent5BlogsBrief();    // 取最近5条博客简介
	long getTotalBlogCount();                    // 获取博客总数
	List<BlogBriefVO> getBlogsByPage(int page, int pageSize);  // 分页获取博客
	List<BlogBriefVO> getHotBlogs();             // 获取热门文章
}
