package com.murasame.mapper;

import com.murasame.domain.vo.BlogBriefVO;
import com.murasame.entity.Blogs;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.math.BigInteger;
import java.util.List;

@Mapper
public interface IndexMapper {
	// 选取文章 - 点击链接跳转文章
	@Select("SELECT * FROM blogs WHERE id=#{id}")
	Blogs getBlogById(@Param("id") BigInteger id);

	// 前端词条展示（取五条）
	@Select("SELECT b.id, b.title, LEFT(b.content, 30) AS brief, b.created_at AS created_at, u.nickname AS author" +
			" FROM blogs b LEFT JOIN users u ON b.author_id=u.id" +
			" ORDER BY created_at DESC LIMIT 5")
	List<BlogBriefVO> getRecent5BlogsBrief();

}
