package com.murasame.domain.vo;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 详情 VO：主记录 + 派生字段 + 时间线。
 */
@Data
public class JobApplicationDetailVO {
    private Long id;
    private Long uId;
    private String company;
    private String position;
    private String channel;
    private String channelLabel;
    private String location;
    private String jobUrl;
    private LocalDate appliedAt;
    private String status;
    private String statusLabel;
    private String priority;
    private String priorityLabel;
    private String salaryRange;
    private String resumeVersion;
    private String contact;
    private String nextAction;
    private LocalDate followUpAt;
    private String notes;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /** 已投天数 */
    private Integer appliedDays;
    /** 当前状态已停留天数 */
    private Integer statusDays;
    private Boolean overdue;
    private Boolean dueToday;

    private List<JobApplicationEventVO> events;
}
