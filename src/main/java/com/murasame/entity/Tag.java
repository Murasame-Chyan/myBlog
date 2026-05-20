package com.murasame.entity;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class Tag {
	Integer id;
	@Size(max = 255, message = "标签名不能超过255个字符")
	String tagName;
}