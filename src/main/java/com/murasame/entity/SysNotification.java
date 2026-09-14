package com.murasame.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class SysNotification {
    private Long id;
    private Long userId;
    private String title;
    private String content;
    /** 类型：1=系统通知 2=荣誉授予 3=公告 */
    private Integer type;
    /** 是否已读：0=未读 1=已读 */
    private Integer isRead;
    private LocalDateTime createTime;
}
