package com.murasame.service;

import com.murasame.domain.dto.TagWrapper;
import com.murasame.entity.Blogs;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public interface BlogService {
	String getBlogById_toString(Long id);                               // id取博客to_string for test

	Blogs getBlogById(Long id);                                         // 按id拉取博客 return Blog

	int publishBlog(Long authorId, String title, String content);    // 撰写-发布文章 return BlogId or 0

	int publishBlogWithTags(Long authorId, String title, String content, TagWrapper tags); // 撰写-发布文章带标签

	int dropBlogToBin(Long id);                                         // 删除blogs.id的博文移入回收箱

	int moveAllBlogsToBin();                                             // 一键移入垃圾箱

	int recoverBlogFromBin(Long id);                                    // 恢复blogsBin.id的博文回到blogs

	int updateBlog(Long id, String title, String content);              // 更新blog，自动更新update_at时间

	int updateBlogWithTags(Long id, String title, String content, TagWrapper tags); // 更新blog带标签

	List<Blogs> getBlogsByTagId(Integer tagId);                          // 根据标签ID查询博客

	int incrementReadCount(Long blogId);                                // 增加博客阅读量

	int incrementLikeCount(Long blogId);                                // 增加博客点赞量

	int decrementLikeCount(Long blogId);                                // 减少博客点赞量
}
