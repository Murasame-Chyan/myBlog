package com.murasame.service.impl;

import com.murasame.entity.Blogs;
import com.murasame.mapper.BlogMapper;
import com.murasame.mapper.IndexMapper;
import com.murasame.service.BlogService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.math.BigInteger;

@Service
public class BlogServiceImpl implements BlogService {
	@Resource
	BlogMapper blogMapper;

	@Override
	public String getBlogById_toString(Long id) {
		return blogMapper.getBlogById(id).toString();
	}

	@Override
	public Blogs getBlogById(Long id) {
		return blogMapper.getBlogById(id);
	}

	@Override
	public int publishBlog(Integer authorId, String title, String content){
		Blogs blog = new Blogs();
		blog.setAuthor_id(authorId);
		blog.setTitle(title);
		blog.setContent(content);
		return blogMapper.insertBlog(blog) == 1 ? blog.getId().intValue() : 0;
	}
}
