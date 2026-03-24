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

	@Override
	public int addComment(Long blogId, Long parentCid, Integer authorId, String content) {
		CommentDTO comment = new CommentDTO();
		comment.setBlog_id(blogId);
		comment.setAuthor_id(authorId);
		comment.setContent(content);
		
		Long finalParentCid = parentCid;
		
		if (parentCid != null) {
			Comments parentComment = commentMapper.getCommentById(parentCid);
			if (parentComment != null) {
				if (parentComment.getParent_cid().equals(parentComment.getId())) {
					finalParentCid = parentCid;
				} else {
					finalParentCid = parentComment.getParent_cid();
				}
			}
			comment.setParent_cid(finalParentCid);
		} else {
			comment.setParent_cid(0L);
		}
		
		int result = commentMapper.insertComment(comment);
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

	@Override
	public List<CommentVO> getCommentTree(Long blogId) {
		List<Comments> allComments = commentMapper.getCommentsByBlogId(blogId);
		List<CommentVO> allVOs = convertToVOList(allComments);
		
		Map<Long, CommentVO> commentMap = new HashMap<>();
		List<CommentVO> rootComments = new ArrayList<>();

		for (CommentVO comment : allVOs) {
			commentMap.put(comment.getId(), comment);
		}

		for (CommentVO comment : allVOs) {
			Long parentId = comment.getParent_cid();
			if (parentId != null && parentId.equals(comment.getId())) {
				rootComments.add(comment);
			} else if (parentId != null && commentMap.containsKey(parentId)) {
				CommentVO parentComment = commentMap.get(parentId);
				if (parentComment.getChildren() == null) {
					parentComment.setChildren(new ArrayList<>());
				}
				parentComment.getChildren().add(comment);
			}
		}

		return rootComments;
	}

	private List<CommentVO> convertToVOList(List<Comments> comments) {
		List<CommentVO> vos = new ArrayList<>();
		for (Comments comment : comments) {
			CommentVO vo = new CommentVO();
			vo.setId(comment.getId());
			vo.setBlog_id(comment.getBlog_id());
			vo.setParent_cid(comment.getParent_cid());
			vo.setAuthor_id(comment.getAuthor_id());
			String nickname = userMapper.getNicknameById(comment.getAuthor_id().longValue());
			vo.setAuthor_name(nickname != null ? nickname : "未知用户");
			vo.setContent(comment.getContent());
			vo.setCreated_at(comment.getCreated_at());
			vos.add(vo);
		}
		return vos;
	}
}
