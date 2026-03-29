package com.murasame.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class Comments {
	Long id;
	Long b_id;
	Long parent_cid;
	Long u_id;
	String content;
	LocalDateTime created_at;
}
