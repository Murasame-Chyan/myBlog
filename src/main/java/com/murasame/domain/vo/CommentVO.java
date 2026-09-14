package com.murasame.domain.vo;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class CommentVO {
	Long id;
	Long b_id;
	Long parent_cid;
	Long u_id;
	String author_name;
	String author_avatar;
	Integer authorLevel;
	List<Integer> authorAchievementIds;
	String content;
	LocalDateTime created_at;
	Integer is_pinned;  // 是否置顶：0=否 1=是
	List<CommentVO> children;
}
