package com.murasame.service.impl;

import com.murasame.domain.dto.TagWrapper;
import com.murasame.entity.Blogs;
import com.murasame.mapper.BlogMapper;
import com.murasame.service.BlogService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

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
	public int publishBlog(Long authorId, String title, String content){ // 发布博文，成功返回博文id，否则返回0
		Blogs blog = new Blogs();
		blog.setU_id(authorId);
		blog.setTitle(title.substring(0, Math.min(title.length(), 255)));   // 标题限长
		blog.setContent(content);
		return blogMapper.insertBlog(blog) == 1 ? blog.getId().intValue() : 0;
	}

	@Override
	public int publishBlogWithTags(Long authorId, String title, String content, TagWrapper tags) {
		Blogs blog = new Blogs();
		blog.setU_id(authorId);
		blog.setTitle(title.substring(0, Math.min(title.length(), 255)));   // 标题限长
		blog.setContent(content);
		blog.setT_id(tags);
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

	private static <T> List<List<T>> splitList(List<T> list, int size) {    // 自定义List分片函数
		List<List<T>> result = new ArrayList<>();
		for (int i = 0; i < list.size(); i += size) {
			result.add(list.subList(i, Math.min(i + size, list.size())));
		}
		return result;
	}

	@Override
	public int moveAllBlogsToBin(){ // 全体blog缓存后删除，全部移入垃圾箱
		List<Blogs> droppingBlogs = blogMapper.getAllBlogs();
		if (droppingBlogs.isEmpty()) {
			return 0;
		}
		int sum = 0;
		// 每 500 条一批(性能优化)
		for (List<Blogs> batch : splitList(droppingBlogs, 500)) {
			sum += blogMapper.dropAllBlogsToBin(batch);
		}
		blogMapper.removeAllBlogs();
		return sum;
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

	@Override
	public int updateBlogWithTags(Long id, String title, String content, TagWrapper tags) {
		int result = blogMapper.editBlog(id, title, content);
		if (result > 0 && tags != null) {
			blogMapper.updateBlogTags(id, tags);
		}
		return result;
	}

	@Override
	public List<Blogs> getBlogsByTagId(Integer tagId) {
		return blogMapper.getBlogsByTagId(tagId);
	}

	@Override
	public int incrementReadCount(Long blogId) {
		return blogMapper.incrementReadCount(blogId);
	}

	@Override
	public int incrementLikeCount(Long blogId) {
		return blogMapper.incrementLikeCount(blogId);
	}

	@Override
	public int decrementLikeCount(Long blogId) {
		return blogMapper.decrementLikeCount(blogId);
	}
}
