package com.murasame.entity;

import lombok.Data;

import java.math.BigInteger;
import java.time.LocalDateTime;

@Data
public class Blogs {
	BigInteger id;
	Integer author_id;
	LocalDateTime created_at;
	LocalDateTime updated_at;
	String title;
	String content;
}
