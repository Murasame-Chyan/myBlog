package com.murasame.mapper;

import com.murasame.domain.dto.CommentDTO;
import com.murasame.domain.vo.CommentVO;
import com.murasame.entity.Comments;
import org.apache.ibatis.annotations.*;

import java.util.List;
import java.util.Map;

@Mapper
public interface CommentMapper {
	@Insert("INSERT INTO comments(b_id, parent_cid, u_id, content, created_at)" +
			"VALUES (#{comment.b_id}, #{comment.parent_cid}, #{comment.u_id}, #{comment.content}, NOW())")
	@Options(useGeneratedKeys = true, keyProperty = "id")
	int insertComment(@Param("comment") CommentDTO comment);

	@Results(id = "commentResultMap", value = {
		@Result(property = "id", column = "id"),
		@Result(property = "b_id", column = "b_id"),
		@Result(property = "parent_cid", column = "parent_cid"),
		@Result(property = "u_id", column = "u_id"),
		@Result(property = "content", column = "content"),
		@Result(property = "created_at", column = "created_at")
	})
	@Select("SELECT * FROM comments WHERE b_id=#{blogId} ORDER BY created_at ASC")
	List<Comments> getCommentsByBlogId(@Param("blogId") Long blogId);

	@Results(id = "commentVOResultMap", value = {
		@Result(property = "id", column = "id"),
		@Result(property = "b_id", column = "b_id"),
		@Result(property = "parent_cid", column = "parent_cid"),
		@Result(property = "u_id", column = "u_id"),
		@Result(property = "author_name", column = "author_name"),
		@Result(property = "content", column = "content"),
		@Result(property = "created_at", column = "created_at")
	})
	// COALESCE 兜底：用户被删除后评论仍可展示，author_name 显示为"未知用户"
	@Select("SELECT c.*, COALESCE(u.nickname, '未知用户') as author_name " +
			"FROM comments c " +
			"LEFT JOIN users u ON c.u_id = u.id " +
			"ORDER BY c.created_at DESC " +
			"LIMIT #{limit}")
	List<CommentVO> getRecentComments(@Param("limit") int limit);

	@ResultMap("commentResultMap")
	@Select("SELECT * FROM comments WHERE id=#{id}")
	Comments getCommentById(@Param("id") Long id);

	@ResultMap("commentResultMap")
	@Select("SELECT * FROM comments WHERE parent_cid=#{parentCid} ORDER BY created_at ASC")
	List<Comments> getChildComments(@Param("parentCid") Long parentCid);

	@ResultMap("commentResultMap")
	@Select("SELECT * FROM comments WHERE b_id=#{blogId} AND parent_cid=#{parentCid} ORDER BY created_at ASC")
	List<Comments> getCommentsByBlogAndParent(@Param("blogId") Long blogId, @Param("parentCid") Long parentCid);

	@Update("UPDATE comments SET parent_cid=#{parentCid} WHERE id=#{id}")
	int updateParentCid(@Param("id") Long id, @Param("parentCid") Long parentCid);

	@Select("SELECT COUNT(*) FROM comments WHERE b_id=#{blogId}")
	int getCommentCountByBlogId(@Param("blogId") Long blogId);

	/**
	 * 获取近N天每日评论数
	 */
	@Select("""
		SELECT
			DATE(c.created_at) as date,
			COUNT(*) as comment_count
		FROM comments c
		INNER JOIN blogs b ON c.b_id = b.id
		WHERE b.u_id = #{userId}
		  AND c.created_at >= DATE_SUB(CURDATE(), INTERVAL #{days} DAY)
		GROUP BY DATE(c.created_at)
		ORDER BY date ASC
	""")
	List<Map<String, Object>> getDailyCommentTrend(@Param("userId") Long userId, @Param("days") int days);

	/**
	 * 获取自定义日期范围的每日评论数趋势
	 */
	@Select("""
		SELECT
			DATE(c.created_at) as date,
			COUNT(*) as comment_count
		FROM comments c
		INNER JOIN blogs b ON c.b_id = b.id
		WHERE b.u_id = #{userId}
		  AND c.created_at &gt;= #{startDate}
		  AND c.created_at &lt; #{endDate} + INTERVAL 1 DAY
		GROUP BY DATE(c.created_at)
		ORDER BY date ASC
	""")
	List<Map<String, Object>> getDailyCommentTrendByRange(@Param("userId") Long userId,
			@Param("startDate") String startDate, @Param("endDate") String endDate);
}

