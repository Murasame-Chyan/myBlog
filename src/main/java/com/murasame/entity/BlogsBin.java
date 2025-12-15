package com.murasame.entity;

import lombok.Data;

import java.time.LocalDateTime;

// 删除博文垃圾箱（暂定7天有效期-未实现）
@Data
public class BlogsBin {
	Long id;
	Integer author_id;
	LocalDateTime created_at;
	LocalDateTime updated_at;
	String title;
	String content;
	LocalDateTime deleted_at;
}
