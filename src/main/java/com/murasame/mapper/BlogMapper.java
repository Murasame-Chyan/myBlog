package com.murasame.mapper;

import com.murasame.entity.Blogs;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface BlogMapper {
	// 选取文章 - 点击链接跳转文章
	@Select("SELECT * FROM blogs WHERE id=#{id}")
	Blogs getBlogById(@Param("id") Long id);

	// 内部 - 获取所有博客
	@Select("SELECT * FROM blogs")
	List<Blogs> getAllBlogs();

	// 收录博文（撰写-发布文章）
	@Insert("INSERT INTO blogs(author_id, title, content, created_at, updated_at)" +
			"VALUES (#{blog.author_id}, #{blog.title}, #{blog.content}, NOW(), NOW())")
	@Options(useGeneratedKeys = true, keyProperty = "id")
	int insertBlog(@Param("blog") Blogs blog);

	// 删除与从垃圾箱恢复博文
	@Select("SELECT * FROM blogsBin WHERE id=#{id}")
	Blogs getBlogFromBinById(@Param("id") Long id);

	@Delete("DELETE FROM blogs WHERE id=#{id}")
	int deleteBlogById(@Param("id") Long id);

	@Insert("INSERT INTO blogsBin(id, author_id, title, content, created_at, updated_at, deleted_at)" +
			"VALUES (#{blog.id}, #{blog.author_id}, #{blog.title}, #{blog.content}, #{blog.created_at}, #{blog.updated_at}, NOW())")
	int dropBlogsToBin(@Param("blog") Blogs blog);

	// 批量移入垃圾箱
	@Insert("""
	<script>
		INSERT INTO blogsBin(id, author_id, title, content, created_at, updated_at, deleted_at) VALUES 
		<foreach collection='blogList' item='blog' separator=','>
		    (#{blog.id}, #{blog.author_id}, #{blog.title}, #{blog.content}, #{blog.created_at}, #{blog.updated_at}, NOW())
		</foreach>
	</script>
	""")
	int dropAllBlogsToBin(@Param("blogList") List<Blogs> blogList);

	@Delete("DELETE FROM blogsBin WHERE id=#{id}")
	int removeBlogFromBin(@Param("id") Long id);

	@Delete("DELETE FROM blogs")
	int removeAllBlogs();

	@Insert("INSERT INTO blogs(author_id, title, content, created_at, updated_at)" +
	"VALUES (#{blog.author_id}, #{blog.title}, #{blog.content}, #{blog.created_at}, NOW())")
	@Options(useGeneratedKeys = true, keyProperty = "id")
	int recoverBlogFromBin(@Param("blog") Blogs blog);

	// 更新博文
	@Update("UPDATE blogs SET title=#{title}, content=#{content}, updated_at=NOW() WHERE id=#{id}")
	int editBlog(@Param("id") Long id,
	             @Param("title") String title,
	             @Param("content") String content);
}
