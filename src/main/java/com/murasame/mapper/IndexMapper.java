package com.murasame.mapper;

import com.murasame.domain.vo.BlogBriefVO;
import com.murasame.handler.TagWrapperTypeHandler;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface IndexMapper {
	@Results(id = "blogBriefResultMap", value = {
		@Result(property = "id", column = "id"),
		@Result(property = "u_id", column = "u_id"),
		@Result(property = "title", column = "title"),
		@Result(property = "brief", column = "brief"),
		@Result(property = "cover_image", column = "cover_image"),
		@Result(property = "created_at", column = "created_at"),
		@Result(property = "updated_at", column = "updated_at"),
		@Result(property = "author", column = "author"),
		@Result(property = "author_avatar", column = "author_avatar"),
		@Result(property = "t_id", column = "t_id", typeHandler = TagWrapperTypeHandler.class),
		@Result(property = "read_count", column = "read_count"),
		@Result(property = "like_count", column = "like_count"),
		@Result(property = "comment_count", column = "comment_count")
	})
	// 三表LEFT JOIN：blogs ← users(取作者昵称) ← 评论子查询(COUNT聚合，COALESCE确保无评论时显示0)
	@Select("SELECT b.id, b.u_id, b.title, LEFT(b.content, 500) AS brief, b.cover_image, b.created_at AS created_at, b.updated_at AS updated_at, u.nickname AS author, u.avatar AS author_avatar, b.t_id, b.read_count, b.like_count, COALESCE(c.comment_count, 0) AS comment_count" +
			" FROM blogs b LEFT JOIN users u ON b.u_id=u.id LEFT JOIN (SELECT b_id, COUNT(*) AS comment_count FROM comments GROUP BY b_id) c ON b.id=c.b_id" +
			" ORDER BY created_at DESC LIMIT 5")
	List<BlogBriefVO> getRecent5BlogsBrief();

	@Select("SELECT COUNT(*) FROM blogs")
	long getTotalBlogCount();

	@ResultMap("blogBriefResultMap")
	@Select("SELECT b.id, b.u_id, b.title, LEFT(b.content, 500) AS brief, b.cover_image, b.created_at AS created_at, b.updated_at AS updated_at, u.nickname AS author, u.avatar AS author_avatar, b.t_id, b.read_count, b.like_count, COALESCE(c.comment_count, 0) AS comment_count" +
			" FROM blogs b LEFT JOIN users u ON b.u_id=u.id LEFT JOIN (SELECT b_id, COUNT(*) AS comment_count FROM comments GROUP BY b_id) c ON b.id=c.b_id" +
			" ORDER BY created_at DESC LIMIT #{pageSize} OFFSET #{offset}")
	List<BlogBriefVO> getBlogsByPage(int pageSize, int offset);

	// 热门博客按 (阅读量 + 点赞数) 降序排列，与上方分页查询使用相同的三表JOIN结构
	@ResultMap("blogBriefResultMap")
	@Select("SELECT b.id, b.u_id, b.title, LEFT(b.content, 500) AS brief, b.cover_image, b.created_at AS created_at, b.updated_at AS updated_at, u.nickname AS author, u.avatar AS author_avatar, b.t_id, b.read_count, b.like_count, COALESCE(c.comment_count, 0) AS comment_count" +
			" FROM blogs b LEFT JOIN users u ON b.u_id=u.id LEFT JOIN (SELECT b_id, COUNT(*) AS comment_count FROM comments GROUP BY b_id) c ON b.id=c.b_id" +
			" ORDER BY (b.read_count + b.like_count) DESC LIMIT 5")
	List<BlogBriefVO> getHotBlogs();
}
