package com.murasame.entity;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 求职投递时间线事件（对应表 job_application_event）。
 * 只追加、不编辑；type 取值见 com.murasame.util.CareerEnums.EVENT_TYPES。
 */
@Data
public class JobApplicationEvent {
    private Long id;
    private Long applicationId;
    private Long uId;
    private String type;
    private LocalDateTime occurredAt;
    private String fromStatus;
    private String toStatus;
    private String title;
    private String content;
    private String meta;
    private LocalDateTime createdAt;
}
