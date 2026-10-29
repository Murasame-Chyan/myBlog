package com.murasame.domain.vo;

import lombok.Data;

import java.util.List;

/**
 * 统计 VO：总数 / 逾期 / 今日跟进 / 各状态分布 / 各渠道分布。
 */
@Data
public class JobApplicationStatsVO {
    private long total;
    private long overdue;
    private long dueToday;
    private List<CountItemVO> byStatus;
    private List<CountItemVO> byChannel;
}
