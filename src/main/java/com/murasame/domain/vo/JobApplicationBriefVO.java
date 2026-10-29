package com.murasame.domain.vo;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 列表项 VO：库表字段 + 中文标签 + 派生字段。
 */
@Data
public class JobApplicationBriefVO {
    private Long id;
    private Long uId;
    private String company;
    private String position;
    private String channel;
    private String channelLabel;
    private String status;
    private String statusLabel;
    private String priority;
    private String priorityLabel;
    private String location;
    /** JD 链接（列表里渠道可点击跳转，前端只认 http/https） */
    private String jobUrl;
    private LocalDate appliedAt;
    private LocalDate followUpAt;
    private String nextAction;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /** 已投天数 */
    private Integer appliedDays;
    /** 是否逾期跟进 */
    private Boolean overdue;
    /** 是否今日跟进 */
    private Boolean dueToday;
    /** 软删除时间（仅回收站列表使用） */
    private LocalDateTime deletedAt;
}
