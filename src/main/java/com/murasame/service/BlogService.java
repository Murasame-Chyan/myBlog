package com.murasame.service;

import com.murasame.entity.Blogs;
import org.springframework.stereotype.Service;

@Service
public interface BlogService {
	String getBlogById_toString(Long id);                               // id取博客to_string for test

	Blogs getBlogById(Long id);                                         // 按id拉取博客 return Blog

	int publishBlog(Integer authorId, String title, String content);    // 撰写-发布文章 return BlogId or 0

	int dropBlogToBin(Long id);                                         // 删除blogs.id的博文移入回收箱

	int moveAllBlogsToBin();                                             // 一键移入垃圾箱

	int recoverBlogFromBin(Long id);                                    // 恢复blogsBin.id的博文回到blogs

	int updateBlog(Long id, String title, String content);              // 更新blog，自动更新update_at时间
}
