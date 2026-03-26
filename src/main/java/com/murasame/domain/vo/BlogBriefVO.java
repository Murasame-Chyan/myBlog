package com.murasame.domain.vo;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.murasame.domain.dto.TagWrapper;
import lombok.Data;

import java.time.LocalDateTime;

// 博客摘要词条 - 前端展示VO
@Data
public class BlogBriefVO {
	private Long id;              // blogs.id
	private String title;               // blogs.title
	private String brief;               // top 30 char of blogs.content
	private LocalDateTime created_at;   // same
	private LocalDateTime updated_at;
	private String author;              // users.nickname
	@JsonInclude(JsonInclude.Include.NON_NULL)
	TagWrapper t_id;              // 标签列表
}
