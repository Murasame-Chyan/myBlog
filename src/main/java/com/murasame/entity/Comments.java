package com.murasame.entity;

import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class Comments {
	Long id;
	Long b_id;
	Long parent_cid;
	Long u_id;
	@Size(max = 65535, message = "评论内容过长")
	String content;
	LocalDateTime created_at;
}
