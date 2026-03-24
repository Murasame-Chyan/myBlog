package com.murasame.mapper;

import com.murasame.domain.dto.CommentDTO;
import com.murasame.domain.vo.CommentVO;
import com.murasame.entity.Comments;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface CommentMapper {
	@Insert("INSERT INTO comments(blog_id, parent_cid, author_id, content, created_at)" +
			"VALUES (#{comment.blog_id}, #{comment.parent_cid}, #{comment.author_id}, #{comment.content}, NOW())")
	@Options(useGeneratedKeys = true, keyProperty = "id")
	int insertComment(@Param("comment") CommentDTO comment);

	@Select("SELECT * FROM comments WHERE blog_id=#{blogId} ORDER BY created_at ASC")
	List<Comments> getCommentsByBlogId(@Param("blogId") Long blogId);

	@Select("SELECT c.*, COALESCE(u.nickname, '未知用户') as author_name " +
			"FROM comments c " +
			"LEFT JOIN users u ON c.author_id = u.id " +
			"ORDER BY c.created_at DESC " +
			"LIMIT #{limit}")
	List<CommentVO> getRecentComments(@Param("limit") int limit);

	@Select("SELECT * FROM comments WHERE id=#{id}")
	Comments getCommentById(@Param("id") Long id);

	@Select("SELECT * FROM comments WHERE parent_cid=#{parentCid} ORDER BY created_at ASC")
	List<Comments> getChildComments(@Param("parentCid") Long parentCid);

	@Select("SELECT * FROM comments WHERE blog_id=#{blogId} AND parent_cid=#{parentCid} ORDER BY created_at ASC")
	List<Comments> getCommentsByBlogAndParent(@Param("blogId") Long blogId, @Param("parentCid") Long parentCid);

	@Update("UPDATE comments SET parent_cid=#{parentCid} WHERE id=#{id}")
	int updateParentCid(@Param("id") Long id, @Param("parentCid") Long parentCid);
}
