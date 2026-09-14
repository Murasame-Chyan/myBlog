package com.murasame.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class SysAnnouncement {
    private Long id;
    private String title;
    private String content;
    /** 状态：0=草稿 1=已发布 2=已撤回 */
    private Integer status;
    private Long publisherId;
    private LocalDateTime createTime;
    private LocalDateTime publishTime;
}
