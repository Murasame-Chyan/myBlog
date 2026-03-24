package com.murasame.domain.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class CommentDTO {
	Long id;
	Long blog_id;
	Long parent_cid;
	Integer author_id;
	String content;
	LocalDateTime created_at;
}
