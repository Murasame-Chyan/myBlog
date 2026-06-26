package com.murasame.domain.vo;

import lombok.Data;
import java.util.List;

/**
 * 创作者数据分析VO
 */
@Data
public class CreatorAnalyticsVO {
    // 核心数据看板
    private Long totalBlogs;          // 总博客数
    private Long totalReads;          // 总阅读量
    private Long totalLikes;          // 总点赞数
    private Long totalComments;       // 总评论数

    // 趋势数据（用于ECharts折线图）
    private List<String> trendDates;  // 日期数组 ["2026-05-29", "2026-05-30", ...]
    private List<Long> trendReads;    // 每日阅读量
    private List<Long> trendLikes;    // 每日点赞量
    private List<Long> trendComments; // 每日评论量

    // 热门文章Top10
    private List<HotBlogItem> hotBlogs;

    // 标签分析
    private List<TagAnalyticsItem> tagAnalytics;

    // 互动率
    private Double likeRate;          // 点赞率 = 总点赞/总阅读
    private Double commentRate;       // 评论率 = 总评论/总阅读

    // 发文热力图数据（365天）
    private List<HeatmapDay> publishHeatmap;

    @Data
    public static class HotBlogItem {
        private Long id;
        private String title;
        private Long readCount;
        private Long likeCount;
        private Double score;  // 综合得分 = readCount * 0.5 + likeCount * 2
    }

    @Data
    public static class TagAnalyticsItem {
        private Long tagId;
        private String tagName;
        private Long blogCount;    // 该标签下文章数
        private Double avgReads;   // 平均阅读量
    }

    @Data
    public static class HeatmapDay {
        private String date;       // "2025-06-05"
        private Long publishCount; // 当天发文数
    }
}

