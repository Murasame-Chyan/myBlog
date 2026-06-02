package com.murasame.entity;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.murasame.domain.dto.TagWrapper;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class Blogs {
	Long id;
	Long u_id;
	LocalDateTime created_at;
	LocalDateTime updated_at;
	@Size(max = 255, message = "标题不能超过255个字符")
	String title;
	String content;
	String cover_image;  // 封面图片URL
	@JsonInclude(JsonInclude.Include.NON_NULL)
	TagWrapper t_id;
	Long read_count;
	Long like_count;
}
