package com.murasame.entity;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.murasame.domain.dto.TagWrapper;
import lombok.Data;

import java.time.LocalDateTime;

// 删除博文垃圾箱（暂定7天有效期-未实现）
@Data
public class BlogsBin {
	Long id;
	Integer u_id;
	LocalDateTime created_at;
	LocalDateTime updated_at;
	String title;
	String content;
	LocalDateTime deleted_at;
	@JsonInclude(JsonInclude.Include.NON_NULL)
	TagWrapper t_id;
}
