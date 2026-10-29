package com.murasame.domain.vo;

import lombok.Data;

import java.util.List;

/**
 * 列表页数据：分页列表 + 置顶的"今日跟进/逾期跟进"分组。
 */
@Data
public class JobApplicationPageVO {
    private List<JobApplicationBriefVO> list;
    private long total;
    private int page;
    private int pageSize;
    /** 今日跟进（最多 20 条） */
    private List<JobApplicationBriefVO> dueTodayList;
    /** 逾期跟进（最多 20 条） */
    private List<JobApplicationBriefVO> overdueList;
}
