package com.murasame.domain.vo;

import lombok.Data;

import java.time.LocalDateTime;

// 博客摘要词条 - 前端展示VO
@Data
public class BlogBriefVO {
	private Long id;              // blogs.id
	private String title;               // blogs.title
	private String brief;               // top 30 char of blogs.content
	private LocalDateTime created_at;   // same
	private String author;              // users.nickname
}
