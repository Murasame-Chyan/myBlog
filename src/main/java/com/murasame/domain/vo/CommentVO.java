package com.murasame.domain.vo;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class CommentVO {
	Long id;
	Long b_id;
	Long parent_cid;
	Long u_id;
	String author_name;
	String content;
	LocalDateTime created_at;
	List<CommentVO> children;
}
