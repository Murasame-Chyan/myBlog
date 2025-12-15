package com.murasame.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class Comments {
	Long id;
	Long blog_id;
	Long parent_cid;
	Integer author_id;
	String content;
	LocalDateTime created_at;
}
