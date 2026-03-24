package com.murasame.mapper;

import com.murasame.domain.vo.BlogBriefVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface ArchiveMapper {
	@Select("SELECT b.id, b.title, LEFT(b.content, 30) AS brief, b.created_at AS created_at, b.updated_at AS updated_at, u.nickname AS author" +
			" FROM blogsBin b LEFT JOIN users u ON b.author_id=u.id" +
			" ORDER BY deleted_at DESC LIMIT 5")
	List<BlogBriefVO> getRecent5ArchivesBrief();

	@Select("SELECT COUNT(*) FROM blogsBin")
	long getTotalArchiveCount();

	@Select("SELECT b.id, b.title, LEFT(b.content, 30) AS brief, b.created_at AS created_at, b.updated_at AS updated_at, u.nickname AS author" +
			" FROM blogsBin b LEFT JOIN users u ON b.author_id=u.id" +
			" ORDER BY deleted_at DESC LIMIT #{pageSize} OFFSET #{offset}")
	List<BlogBriefVO> getArchivesByPage(int pageSize, int offset);

	@Select("SELECT * FROM blogsBin WHERE id=#{id}")
	com.murasame.entity.BlogsBin getArchiveById(@Param("id") Long id);
}
