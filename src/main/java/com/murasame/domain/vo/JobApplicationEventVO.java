package com.murasame.domain.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 时间线事件 VO。
 */
@Data
public class JobApplicationEventVO {
    private Long id;
    private String type;
    private String typeLabel;
    private LocalDateTime occurredAt;
    private String fromStatus;
    private String fromStatusLabel;
    private String toStatus;
    private String toStatusLabel;
    private String title;
    private String content;
    private String meta;
    private LocalDateTime createdAt;
}
