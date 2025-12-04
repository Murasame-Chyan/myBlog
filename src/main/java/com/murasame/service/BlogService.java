package com.murasame.service;

import com.murasame.entity.Blogs;
import org.springframework.stereotype.Service;

import java.math.BigInteger;

@Service
public interface BlogService {
	String getBlogById_toString(Long id);                               // id取博客to_string for test

	Blogs getBlogById(Long id);                                         // 按id拉取博客 return Blog

	int publishBlog(Integer authorId, String title, String content);    // 撰写-发布文章 return BlogId or 0
}
