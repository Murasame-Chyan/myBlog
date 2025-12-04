package com.murasame.mapper;

import com.murasame.entity.Blogs;
import org.apache.ibatis.annotations.*;

import java.math.BigInteger;

@Mapper
public interface BlogMapper {
	// 选取文章 - 点击链接跳转文章
	@Select("SELECT * FROM blogs WHERE id=#{id}")
	Blogs getBlogById(@Param("id") Long id);

	// 收录博文（撰写-发布文章）
	@Insert("INSERT INTO blogs(author_id, title, content, created_at, updated_at)" +
			"VALUES (#{blog.author_id}, #{blog.title}, #{blog.content}, NOW(), NOW())")
	@Options(useGeneratedKeys = true, keyProperty = "id")
	int insertBlog(@Param("blog") Blogs blog);
}
