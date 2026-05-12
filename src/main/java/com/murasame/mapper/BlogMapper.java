package com.murasame.mapper;

import com.murasame.domain.dto.TagWrapper;
import com.murasame.domain.vo.BlogBriefVO;
import com.murasame.entity.Blogs;
import com.murasame.handler.TagWrapperTypeHandler;
import org.apache.ibatis.annotations.*;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface BlogMapper {
	// 选取文章 - 点击链接跳转文章
	@Results(id = "blogResultMap", value = {
		@Result(property = "id", column = "id"),
		@Result(property = "u_id", column = "u_id"),
		@Result(property = "created_at", column = "created_at"),
		@Result(property = "updated_at", column = "updated_at"),
		@Result(property = "title", column = "title"),
		@Result(property = "content", column = "content"),
		@Result(property = "t_id", column = "t_id", typeHandler = TagWrapperTypeHandler.class),
		@Result(property = "read_count", column = "read_count"),
		@Result(property = "like_count", column = "like_count")
	})
	@Select("SELECT b.* FROM blogs b WHERE b.id=#{id}")
	Blogs getBlogById(@Param("id") Long id);

	// 内部 - 获取所有博客
	@ResultMap("blogResultMap")
	@Select("SELECT * FROM blogs")
	List<Blogs> getAllBlogs();

	// 收录博文（撰写-发布文章）
	@Insert("INSERT INTO blogs(u_id, title, content, created_at, updated_at, t_id)" +
			"VALUES (#{blog.u_id}, #{blog.title}, #{blog.content}, NOW(), NOW(), #{blog.t_id, typeHandler=com.murasame.handler.TagWrapperTypeHandler})")
	@Options(useGeneratedKeys = true, keyProperty = "id")
	int insertBlog(@Param("blog") Blogs blog);

	// 删除与从垃圾箱恢复博文
	@Select("SELECT * FROM blogsBin WHERE id=#{id}")
	Blogs getBlogFromBinById(@Param("id") Long id);

	@Delete("DELETE FROM blogs WHERE id=#{id}")
	int deleteBlogById(@Param("id") Long id);

	@Insert("INSERT INTO blogsBin(id, u_id, title, content, created_at, updated_at, deleted_at, t_id)" +
			"VALUES (#{blog.id}, #{blog.u_id}, #{blog.title}, #{blog.content}, #{blog.created_at}, #{blog.updated_at}, NOW(), #{blog.t_id, typeHandler=com.murasame.handler.TagWrapperTypeHandler})")
	int dropBlogsToBin(@Param("blog") Blogs blog);

	// 批量移入垃圾箱
	@Insert("<script>"
		+ "INSERT INTO blogsBin(id, u_id, title, content, created_at, updated_at, deleted_at, t_id) VALUES "
		+ "<foreach collection='blogList' item='blog' separator=','>"
		+ "(#{blog.id}, #{blog.u_id}, #{blog.title}, #{blog.content}, #{blog.created_at}, #{blog.updated_at}, NOW(), #{blog.t_id, typeHandler=com.murasame.handler.TagWrapperTypeHandler})"
		+ "</foreach>"
		+ "</script>")
	int dropAllBlogsToBin(@Param("blogList") List<Blogs> blogList);

	@Delete("DELETE FROM blogsBin WHERE id=#{id}")
	int removeBlogFromBin(@Param("id") Long id);

	@Delete("DELETE FROM blogs")
	int removeAllBlogs();

	@Insert("INSERT INTO blogs(u_id, title, content, created_at, updated_at, t_id)" +
	"VALUES (#{blog.u_id}, #{blog.title}, #{blog.content}, #{blog.created_at}, NOW(), #{blog.t_id, typeHandler=com.murasame.handler.TagWrapperTypeHandler})")
	@Options(useGeneratedKeys = true, keyProperty = "id")
	int recoverBlogFromBin(@Param("blog") Blogs blog);

	// 更新博文
	@Update("UPDATE blogs SET title=#{title}, content=#{content}, updated_at=NOW() WHERE id=#{id}")
	int editBlog(@Param("id") Long id,
	             @Param("title") String title,
	             @Param("content") String content);

	// 更新博文标签
	@Update("UPDATE blogs SET t_id=#{tags, typeHandler=com.murasame.handler.TagWrapperTypeHandler} WHERE id=#{id}")
	int updateBlogTags(@Param("id") Long id, @Param("tags") TagWrapper tags);

	// 根据标签ID查询博客
	@ResultMap("blogResultMap")
	@Select("SELECT * FROM blogs WHERE JSON_CONTAINS(t_id, CAST(#{tagId} AS JSON), '$.tagList')")
	List<Blogs> getBlogsByTagId(@Param("tagId") Integer tagId);

	// 增加博客阅读量
	@Update("UPDATE blogs SET read_count = read_count + 1 WHERE id = #{blogId}")
	int incrementReadCount(@Param("blogId") Long blogId);

	// 增加博客点赞量
	@Update("UPDATE blogs SET like_count = like_count + 1 WHERE id = #{blogId}")
	int incrementLikeCount(@Param("blogId") Long blogId);

	// 减少博客点赞量
	@Update("UPDATE blogs SET like_count = like_count - 1 WHERE id = #{blogId}")
	int decrementLikeCount(@Param("blogId") Long blogId);

    @ResultMap("com.murasame.mapper.IndexMapper.blogBriefResultMap")
    // 搜索博客（关键词 + 时间范围 + 排序 + 分页）
    @Select("<script>" +
        "SELECT b.id, b.title, LEFT(b.content,30) AS brief, b.created_at, b.updated_at, " +
        "u.nickname AS author, b.t_id, b.read_count, b.like_count, " +
        "COALESCE(c.comment_count,0) AS comment_count " +
        "FROM blogs b " +
        "LEFT JOIN users u ON b.u_id=u.id " +
        "LEFT JOIN (SELECT b_id, COUNT(*) AS comment_count FROM comments GROUP BY b_id) c ON b.id=c.b_id " +
        "WHERE 1=1 " +
        "<if test='keyword != null and keyword != \"\"'>" +
        "AND (b.title LIKE CONCAT('%',#{keyword},'%') OR b.content LIKE CONCAT('%',#{keyword},'%') OR u.nickname LIKE CONCAT('%',#{keyword},'%')) " +
        "</if>" +
        "<if test='dateFrom != null'>AND b.created_at >= #{dateFrom} </if>" +
        "<if test='dateTo != null'>AND b.created_at &lt;= #{dateTo} </if>" +
        "<choose>" +
        "<when test='sortBy == \"likes\"'>ORDER BY b.like_count DESC</when>" +
        "<when test='sortBy == \"reads\"'>ORDER BY b.read_count DESC</when>" +
        "<when test='sortBy == \"comments\"'>ORDER BY COALESCE(c.comment_count,0) DESC</when>" +
        "<when test='sortBy == \"oldest\"'>ORDER BY b.created_at ASC</when>" +
        "<otherwise>ORDER BY b.created_at DESC</otherwise>" +
        "</choose>" +
        "LIMIT #{pageSize} OFFSET #{offset}" +
        "</script>")
    List<BlogBriefVO> searchBlogs(@Param("keyword") String keyword,
                                  @Param("dateFrom") LocalDateTime dateFrom,
                                  @Param("dateTo") LocalDateTime dateTo,
                                  @Param("sortBy") String sortBy,
                                  @Param("pageSize") int pageSize,
                                  @Param("offset") int offset);

    @Select("<script>" +
        "SELECT COUNT(*) FROM blogs b " +
        "LEFT JOIN users u ON b.u_id=u.id " +
        "WHERE 1=1 " +
        "<if test='keyword != null and keyword != \"\"'>" +
        "AND (b.title LIKE CONCAT('%',#{keyword},'%') OR b.content LIKE CONCAT('%',#{keyword},'%') OR u.nickname LIKE CONCAT('%',#{keyword},'%')) " +
        "</if>" +
        "<if test='dateFrom != null'>AND b.created_at >= #{dateFrom} </if>" +
        "<if test='dateTo != null'>AND b.created_at &lt;= #{dateTo} </if>" +
        "</script>")
    long countSearchBlogs(@Param("keyword") String keyword,
                          @Param("dateFrom") LocalDateTime dateFrom,
                          @Param("dateTo") LocalDateTime dateTo);

    @ResultMap("com.murasame.mapper.IndexMapper.blogBriefResultMap")
    @Select("<script>" +
        "SELECT b.id, b.title, LEFT(b.content,30) AS brief, b.created_at, b.updated_at, " +
        "u.nickname AS author, b.t_id, b.read_count, b.like_count, " +
        "COALESCE(c.comment_count,0) AS comment_count " +
        "FROM blogs b " +
        "LEFT JOIN users u ON b.u_id=u.id " +
        "LEFT JOIN (SELECT b_id, COUNT(*) AS comment_count FROM comments GROUP BY b_id) c ON b.id=c.b_id " +
        "WHERE b.id IN " +
        "<foreach collection='ids' item='id' open='(' separator=',' close=')'>#{id}</foreach>" +
        " ORDER BY b.created_at DESC" +
        "</script>")
    List<BlogBriefVO> getBlogsByIds(@Param("ids") java.util.List<Long> ids);
}
