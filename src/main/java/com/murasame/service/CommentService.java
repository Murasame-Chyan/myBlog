package com.murasame.service;

import com.murasame.domain.vo.CommentVO;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public interface CommentService {
	int addComment(Long blogId, Long parentCid, Integer authorId, String content);

	List<CommentVO> getCommentsByBlogId(Long blogId);

	List<CommentVO> getRecentComments(int limit);

	List<CommentVO> getCommentTree(Long blogId);
}
