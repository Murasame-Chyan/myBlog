package com.murasame.service.impl;

import com.murasame.entity.Blogs;
import com.murasame.mapper.BlogMapper;
import com.murasame.service.BlogService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

@Service
public class BlogServiceImpl implements BlogService {
	@Resource
	private BlogMapper blogMapper;

	@Override
	public String getBlogById_toString(Long id) {
		return blogMapper.getBlogById(id).toString();
	}

	@Override
	public Blogs getBlogById(Long id) {
		return blogMapper.getBlogById(id);
	}

	@Override
	public int publishBlog(Integer authorId, String title, String content){ // 发布博文，成功返回博文id，否则返回0
		Blogs blog = new Blogs();
		blog.setAuthor_id(authorId);
		blog.setTitle(title);
		blog.setContent(content);
		return blogMapper.insertBlog(blog) == 1 ? blog.getId().intValue() : 0;
	}

	@Override
	public int dropBlogToBin(Long id) { // 按id查询要删除的blog，查到则放入回收箱返回1，否则返回0
		Blogs droppingBlog = blogMapper.getBlogById(id);
		if(droppingBlog != null){
			// 删除操作
			blogMapper.deleteBlogById(id);
			blogMapper.dropBlogsToBin(droppingBlog);
			return 1;
		}
		return 0;
	}

	@Override
	public int recoverBlogFromBin(Long id) {
		Blogs recoveringBlog = blogMapper.getBlogFromBinById(id);
		if(recoveringBlog != null){
			// 先删除回收箱再返回正文（blogs.id按最大算）
			blogMapper.removeBlogFromBin(id);
			blogMapper.recoverBlogFromBin(recoveringBlog);
			return 1;
		}
		return 0;
	}

	@Override
	public int updateBlog(Long id, String title, String content){
		return blogMapper.editBlog(id, title, content);
	}
}
