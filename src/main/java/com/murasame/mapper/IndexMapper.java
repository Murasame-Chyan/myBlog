package com.murasame.mapper;

import com.murasame.domain.vo.BlogBriefVO;
import com.murasame.handler.TagWrapperTypeHandler;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface IndexMapper {
	@Results(id = "blogBriefResultMap", value = {
		@Result(property = "id", column = "id"),
		@Result(property = "title", column = "title"),
		@Result(property = "brief", column = "brief"),
		@Result(property = "created_at", column = "created_at"),
		@Result(property = "updated_at", column = "updated_at"),
		@Result(property = "author", column = "author"),
		@Result(property = "t_id", column = "t_id", typeHandler = TagWrapperTypeHandler.class),
		@Result(property = "read_count", column = "read_count"),
		@Result(property = "like_count", column = "like_count"),
		@Result(property = "comment_count", column = "comment_count")
	})
	@Select("SELECT b.id, b.title, LEFT(b.content, 30) AS brief, b.created_at AS created_at, b.updated_at AS updated_at, u.nickname AS author, b.t_id, b.read_count, b.like_count, COALESCE(c.comment_count, 0) AS comment_count" +
			" FROM blogs b LEFT JOIN users u ON b.u_id=u.id LEFT JOIN (SELECT b_id, COUNT(*) AS comment_count FROM comments GROUP BY b_id) c ON b.id=c.b_id" +
			" ORDER BY created_at DESC LIMIT 5")
	List<BlogBriefVO> getRecent5BlogsBrief();

	@Select("SELECT COUNT(*) FROM blogs")
	long getTotalBlogCount();

	@ResultMap("blogBriefResultMap")
	@Select("SELECT b.id, b.title, LEFT(b.content, 30) AS brief, b.created_at AS created_at, b.updated_at AS updated_at, u.nickname AS author, b.t_id, b.read_count, b.like_count, COALESCE(c.comment_count, 0) AS comment_count" +
			" FROM blogs b LEFT JOIN users u ON b.u_id=u.id LEFT JOIN (SELECT b_id, COUNT(*) AS comment_count FROM comments GROUP BY b_id) c ON b.id=c.b_id" +
			" ORDER BY created_at DESC LIMIT #{pageSize} OFFSET #{offset}")
	List<BlogBriefVO> getBlogsByPage(int pageSize, int offset);

	@ResultMap("blogBriefResultMap")
	@Select("SELECT b.id, b.title, LEFT(b.content, 30) AS brief, b.created_at AS created_at, b.updated_at AS updated_at, u.nickname AS author, b.t_id, b.read_count, b.like_count, COALESCE(c.comment_count, 0) AS comment_count" +
			" FROM blogs b LEFT JOIN users u ON b.u_id=u.id LEFT JOIN (SELECT b_id, COUNT(*) AS comment_count FROM comments GROUP BY b_id) c ON b.id=c.b_id" +
			" ORDER BY (b.read_count + b.like_count) DESC LIMIT 5")
	List<BlogBriefVO> getHotBlogs();
}
