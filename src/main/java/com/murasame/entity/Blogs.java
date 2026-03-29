package com.murasame.entity;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.murasame.domain.dto.TagWrapper;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class Blogs {
	Long id;
	Long u_id;
	LocalDateTime created_at;
	LocalDateTime updated_at;
	String title;
	String content;
	@JsonInclude(JsonInclude.Include.NON_NULL)
	TagWrapper t_id;
	Long read_count;
	Long like_count;
}
