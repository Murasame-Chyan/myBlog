package com.murasame.entity;

import lombok.Data;

import java.math.BigInteger;
import java.time.LocalDateTime;

@Data
public class Comments {
	BigInteger id;
	BigInteger blog_id;
	BigInteger parent_cid;
	Integer author_id;
	String content;
	LocalDateTime created_at;
}
