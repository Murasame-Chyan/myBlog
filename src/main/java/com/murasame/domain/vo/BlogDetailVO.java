package com.murasame.domain.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class BlogDetailVO {
    private Long id;
    private Integer u_id;
    private LocalDateTime created_at;
    private LocalDateTime updated_at;
    private String title;
    private String content;
    private TagWrapperVO t_id;
}