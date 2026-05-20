package com.murasame.service.impl;

import com.murasame.domain.dto.CommentDTO;
import com.murasame.domain.vo.CommentVO;
import com.murasame.entity.Comments;
import com.murasame.entity.Users;
import com.murasame.mapper.CommentMapper;
import com.murasame.mapper.UserMapper;
import com.murasame.service.CommentService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class CommentServiceImpl implements CommentService {
	@Resource
	private CommentMapper commentMapper;
	@Resource
	private UserMapper userMapper;

	/**
	 * 添加评论，自动处理嵌套回复的归属逻辑：
	 * 若回复的是根评论（parent_cid == 0），则挂在该根评论下；
	 * 若回复的是子回复（parent_cid != 0），则找到其根评论（祖父节点），
	 * 统一挂到根评论下，保证所有回复只有两层嵌套。
	 */
	@Override
	public int addComment(Long blogId, Long parentCid, Long authorId, String content) {
		CommentDTO comment = new CommentDTO();
		comment.setB_id(blogId);
		comment.setU_id(authorId);
		comment.setContent(content);

		Long finalParentCid = parentCid;

		if (parentCid != null && parentCid != 0) {
			Comments parentComment = commentMapper.getCommentById(parentCid);
			if (parentComment != null) {
				if (parentComment.getParent_cid() == 0) {
					// 父评论本身就是根评论
					finalParentCid = parentComment.getId();
				} else {
					// 父评论是子回复，找到其根评论
					finalParentCid = parentComment.getParent_cid();
				}
			}
			comment.setParent_cid(finalParentCid);
		} else {
			comment.setParent_cid(0L);
		}

		int result = commentMapper.insertComment(comment);
		// 根评论插入后，将 parent_cid 设为自己的 id，方便后续子回复查询
		if (result == 1 && comment.getId() != null && comment.getParent_cid() == 0L) {
			comment.setParent_cid(comment.getId());
			commentMapper.updateParentCid(comment.getId(), comment.getId());
		}
		return result == 1 ? comment.getId().intValue() : 0;
	}

	@Override
	public List<CommentVO> getCommentsByBlogId(Long blogId) {
		List<Comments> comments = commentMapper.getCommentsByBlogId(blogId);
		return convertToVOList(comments);
	}

	@Override
	public List<CommentVO> getRecentComments(int limit) {
		return commentMapper.getRecentComments(limit);
	}

	/**
	 * 将扁平评论列表构建为两级评论树：
	 * 1. 先用 HashMap 做 id -> VO 索引
	 * 2. 遍历所有评论：若 parent_cid == 自己的 id（根评论），加入根列表；
	 *    否则挂到父评论的 children 中。
	 * 只支持两层嵌套（根 + 子回复），不支持更深的层级。
	 */
	@Override
	public List<CommentVO> getCommentTree(Long blogId) {
		List<Comments> allComments = commentMapper.getCommentsByBlogId(blogId);
		List<CommentVO> allVOs = convertToVOList(allComments);

		Map<Long, CommentVO> commentMap = new HashMap<>();
		List<CommentVO> rootComments = new ArrayList<>();

		// 构建 id 索引
		for (CommentVO comment : allVOs) {
			commentMap.put(comment.getId(), comment);
		}

		for (CommentVO comment : allVOs) {
			Long parentId = comment.getParent_cid();
			if (parentId != null && parentId.equals(comment.getId())) {
				// 根评论（parent_cid 指向自己）
				rootComments.add(comment);
			} else if (parentId != null && commentMap.containsKey(parentId)) {
				// 子回复，挂到父评论的 children 列表
				CommentVO parentComment = commentMap.get(parentId);
				if (parentComment.getChildren() == null) {
					parentComment.setChildren(new ArrayList<>());
				}
				parentComment.getChildren().add(comment);
			}
		}

		return rootComments;
	}

	@Override
	public int getCommentCountByBlogId(Long blogId) {
		return commentMapper.getCommentCountByBlogId(blogId);
	}

	private List<CommentVO> convertToVOList(List<Comments> comments) {
		List<CommentVO> vos = new ArrayList<>();
		Map<Long, Users> userCache = new HashMap<>();
		for (Comments comment : comments) {
			CommentVO vo = new CommentVO();
			vo.setId(comment.getId());
			vo.setB_id(comment.getB_id());
			vo.setParent_cid(comment.getParent_cid());
			vo.setU_id(comment.getU_id());
			Users user = userCache.computeIfAbsent(comment.getU_id(), userMapper::getUserById);
			vo.setAuthor_name(user != null ? user.getNickname() : "未知用户");
			vo.setAuthor_avatar(user != null ? user.getAvatar() : null);
			vo.setContent(comment.getContent());
			vo.setCreated_at(comment.getCreated_at());
			vos.add(vo);
		}
		return vos;
	}
}
