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
	@Select("SELECT b.id, b.u_id, b.title, LEFT(b.content, 30) AS brief, b.created_at AS created_at, b.updated_at AS updated_at, u.nickname AS author, u.avatar AS author_avatar, b.t_id" +
			" FROM blogsBin b LEFT JOIN users u ON b.u_id=u.id WHERE b.u_id=#{uId} ORDER BY deleted_at DESC LIMIT 5")
	List<BlogBriefVO> getRecent5ArchivesBrief(@Param("uId") Long uId);

	@Select("SELECT COUNT(*) FROM blogsBin WHERE u_id=#{uId}")
	long getTotalArchiveCount(@Param("uId") Long uId);

	@ResultMap("archiveResultMap")
	@Select("SELECT b.id, b.u_id, b.title, LEFT(b.content, 30) AS brief, b.created_at AS created_at, b.updated_at AS updated_at, u.nickname AS author, u.avatar AS author_avatar, b.t_id" +
			" FROM blogsBin b LEFT JOIN users u ON b.u_id=u.id WHERE b.u_id=#{uId} ORDER BY deleted_at DESC LIMIT #{pageSize} OFFSET #{offset}")
	List<BlogBriefVO> getArchivesByPage(@Param("uId") Long uId, int pageSize, int offset);

	@Select("SELECT * FROM blogsBin WHERE id=#{id}")
	com.murasame.entity.BlogsBin getArchiveById(@Param("id") Long id);

	// 利用MySQL JSON_CONTAINS在tagList数组中查找指定tagId，路径 $.tagList 对应 TagWrapper 结构
	@ResultMap("archiveResultMap")
	@Select("SELECT b.id, b.u_id, b.title, LEFT(b.content, 30) AS brief, b.created_at AS created_at, b.updated_at AS updated_at, u.nickname AS author, u.avatar AS author_avatar" +
			" FROM blogsBin b LEFT JOIN users u ON b.u_id=u.id" +
			" WHERE b.u_id=#{uId} AND JSON_CONTAINS(b.t_id, CAST(#{tagId} AS JSON), '$.tagList')")
	List<BlogBriefVO> getArchivesByTagId(@Param("uId") Long uId, @Param("tagId") Integer tagId);
}
