package com.murasame.mapper;

import com.murasame.domain.vo.BlogBriefVO;
import com.murasame.handler.TagWrapperTypeHandler;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface ArchiveMapper {
	@Results(id = "archiveResultMap", value = {
		@Result(property = "id", column = "id"),
		@Result(property = "created_at", column = "created_at"),
		@Result(property = "updated_at", column = "updated_at"),
		@Result(property = "title", column = "title"),
		@Result(property = "content", column = "content"),
		@Result(property = "deleted_at", column = "deleted_at"),
		@Result(property = "t_id", column = "t_id", typeHandler = TagWrapperTypeHandler.class),
		@Result(property = "read_count", column = "read_count"),
		@Result(property = "like_count", column = "like_count")
	})
	@Select("SELECT b.id, b.title, LEFT(b.content, 30) AS brief, b.created_at AS created_at, b.updated_at AS updated_at, u.nickname AS author, b.t_id" +
			" FROM blogsBin b LEFT JOIN users u ON b.u_id=u.id ORDER BY deleted_at DESC LIMIT 5")
	List<BlogBriefVO> getRecent5ArchivesBrief();

	@Select("SELECT COUNT(*) FROM blogsBin")
	long getTotalArchiveCount();

	@ResultMap("archiveResultMap")
	@Select("SELECT b.id, b.title, LEFT(b.content, 30) AS brief, b.created_at AS created_at, b.updated_at AS updated_at, u.nickname AS author, b.t_id" +
			" FROM blogsBin b LEFT JOIN users u ON b.u_id=u.id ORDER BY deleted_at DESC LIMIT #{pageSize} OFFSET #{offset}")
	List<BlogBriefVO> getArchivesByPage(int pageSize, int offset);

	@Select("SELECT * FROM blogsBin WHERE id=#{id}")
	com.murasame.entity.BlogsBin getArchiveById(@Param("id") Long id);

	@ResultMap("archiveResultMap")
	@Select("SELECT b.id, b.title, LEFT(b.content, 30) AS brief, b.created_at AS created_at, b.updated_at AS updated_at, u.nickname AS author" +
			" FROM blogsBin b LEFT JOIN users u ON b.u_id=u.id" +
			" WHERE JSON_CONTAINS(b.t_id, CAST(#{tagId} AS JSON), '$.tagList')")
	List<BlogBriefVO> getArchivesByTagId(@Param("tagId") Integer tagId);
}
