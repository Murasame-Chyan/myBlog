package com.murasame.domain.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class CommentDTO {
	Long id;
	Long b_id;
	Long parent_cid;
	Integer u_id;
	String content;
	LocalDateTime created_at;
}
