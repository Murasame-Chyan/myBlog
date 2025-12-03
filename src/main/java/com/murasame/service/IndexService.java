package com.murasame.service;

import com.murasame.domain.vo.BlogBriefVO;
import com.murasame.entity.Blogs;
import org.springframework.stereotype.Service;

import java.math.BigInteger;
import java.util.List;

@Service
public interface IndexService {
	String getBlogById_toString(BigInteger id);                  // id取博客to_string for test

	Blogs getBlogById(BigInteger id);                   // 按id拉取博客
	public List<BlogBriefVO> getRecent5BlogsBrief();    // 取最近5条博客简介
}
