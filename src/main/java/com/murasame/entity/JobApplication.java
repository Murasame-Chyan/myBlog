package com.murasame.entity;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 求职投递主记录（对应表 job_application）。
 * 字段用驼峰，依赖 mybatis.configuration.map-underscore-to-camel-case=true 自动映射。
 */
@Data
public class JobApplication {
    private Long id;
    private Long uId;
    private String company;
    private String position;
    private String channel;
    private String location;
    private String jobUrl;
    private LocalDate appliedAt;
    private String status;
    private String priority;
    private String salaryRange;
    private String resumeVersion;
    private String contact;
    private String nextAction;
    private LocalDate followUpAt;
    private String notes;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime deletedAt;
}
