package com.murasame.service.impl;

import com.murasame.domain.vo.CreatorAnalyticsVO;
import com.murasame.mapper.BlogMapper;
import com.murasame.mapper.CommentMapper;
import com.murasame.service.CreatorAnalyticsService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class CreatorAnalyticsServiceImpl implements CreatorAnalyticsService {

    @Resource
    private BlogMapper blogMapper;

    @Resource
    private CommentMapper commentMapper;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    @Override
    public CreatorAnalyticsVO getAnalytics(Long userId, Integer days, String startDate, String endDate, List<Long> tagIds) {
        CreatorAnalyticsVO vo = new CreatorAnalyticsVO();

        // 1. 核心数据看板（不受日期范围影响，始终为总计）
        Map<String, Object> stats = blogMapper.getCreatorStats(userId);
        vo.setTotalBlogs(getLongValue(stats, "total_blogs"));
        vo.setTotalReads(getLongValue(stats, "total_reads"));
        vo.setTotalLikes(getLongValue(stats, "total_likes"));
        vo.setTotalComments(blogMapper.getTotalCommentsByCreator(userId));

        // 2. 计算互动率
        long totalReads = vo.getTotalReads();
        vo.setLikeRate(totalReads > 0 ? vo.getTotalLikes() * 100.0 / totalReads : 0.0);
        vo.setCommentRate(totalReads > 0 ? vo.getTotalComments() * 100.0 / totalReads : 0.0);

        // 3. 趋势数据：支持天数或自定义日期范围
        int trendDays = resolveDays(days, startDate, endDate);
        if (startDate != null && endDate != null) {
            buildTrendDataByRange(vo, userId, startDate, endDate);
        } else {
            buildTrendData(vo, userId, trendDays);
        }

        // 4. 热门文章Top10（不受日期范围影响）
        List<Map<String, Object>> hotData = blogMapper.getHotBlogsTop10(userId);
        vo.setHotBlogs(hotData.stream().map(this::mapToHotBlogItem).collect(Collectors.toList()));

        // 5. 标签分析（支持按标签ID过滤）
        List<Map<String, Object>> tagData;
        if (tagIds != null && !tagIds.isEmpty()) {
            tagData = blogMapper.getTagAnalyticsByIds(userId, tagIds);
        } else {
            tagData = blogMapper.getTagAnalytics(userId);
        }
        vo.setTagAnalytics(tagData.stream().map(this::mapToTagAnalyticsItem).collect(Collectors.toList()));

        // 6. 发文热力图（始终365天）
        List<Map<String, Object>> heatData = blogMapper.getPublishHeatmap(userId);
        vo.setPublishHeatmap(heatData.stream().map(this::mapToHeatmapDay).collect(Collectors.toList()));

        return vo;
    }

    @Override
    public List<Map<String, Object>> getUserTags(Long userId) {
        return blogMapper.getUserTags(userId);
    }

    private int resolveDays(Integer days, String startDate, String endDate) {
        if (startDate != null && endDate != null) {
            try {
                LocalDate start = LocalDate.parse(startDate, DATE_FORMATTER);
                LocalDate end = LocalDate.parse(endDate, DATE_FORMATTER);
                return (int) ChronoUnit.DAYS.between(start, end) + 1;
            } catch (Exception e) {
                // fallback
            }
        }
        return days != null && days > 0 ? days : 30;
    }

    private void buildTrendData(CreatorAnalyticsVO vo, Long userId, int days) {
        List<Map<String, Object>> blogTrend = blogMapper.getTrendData(userId, days);
        List<Map<String, Object>> commentTrend = commentMapper.getDailyCommentTrend(userId, days);

        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusDays(days - 1);
        fillTrendData(vo, blogTrend, commentTrend, startDate, endDate);
    }

    private void buildTrendDataByRange(CreatorAnalyticsVO vo, Long userId, String start, String end) {
        List<Map<String, Object>> blogTrend = blogMapper.getTrendDataByRange(userId, start, end);
        List<Map<String, Object>> commentTrend = commentMapper.getDailyCommentTrendByRange(userId, start, end);

        LocalDate startDate = LocalDate.parse(start, DATE_FORMATTER);
        LocalDate endDate = LocalDate.parse(end, DATE_FORMATTER);
        fillTrendData(vo, blogTrend, commentTrend, startDate, endDate);
    }

    private void fillTrendData(CreatorAnalyticsVO vo,
            List<Map<String, Object>> blogTrend,
            List<Map<String, Object>> commentTrend,
            LocalDate startDate, LocalDate endDate) {

        List<String> dates = new ArrayList<>();
        List<Long> reads = new ArrayList<>();
        List<Long> likes = new ArrayList<>();
        List<Long> comments = new ArrayList<>();

        Map<String, Long> readsMap = blogTrend.stream()
            .collect(Collectors.toMap(
                m -> m.get("date").toString(),
                m -> getLongValue(m, "daily_reads"),
                (a, b) -> b
            ));
        Map<String, Long> likesMap = blogTrend.stream()
            .collect(Collectors.toMap(
                m -> m.get("date").toString(),
                m -> getLongValue(m, "daily_likes"),
                (a, b) -> b
            ));
        Map<String, Long> commentsMap = commentTrend.stream()
            .collect(Collectors.toMap(
                m -> m.get("date").toString(),
                m -> getLongValue(m, "comment_count"),
                (a, b) -> b
            ));

        for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
            String dateStr = date.format(DATE_FORMATTER);
            dates.add(dateStr);
            reads.add(readsMap.getOrDefault(dateStr, 0L));
            likes.add(likesMap.getOrDefault(dateStr, 0L));
            comments.add(commentsMap.getOrDefault(dateStr, 0L));
        }

        vo.setTrendDates(dates);
        vo.setTrendReads(reads);
        vo.setTrendLikes(likes);
        vo.setTrendComments(comments);
    }

    private CreatorAnalyticsVO.HotBlogItem mapToHotBlogItem(Map<String, Object> map) {
        CreatorAnalyticsVO.HotBlogItem item = new CreatorAnalyticsVO.HotBlogItem();
        item.setId(getLongValue(map, "id"));
        item.setTitle((String) map.get("title"));
        item.setReadCount(getLongValue(map, "read_count"));
        item.setLikeCount(getLongValue(map, "like_count"));
        item.setScore(getDoubleValue(map, "score"));
        return item;
    }

    private CreatorAnalyticsVO.TagAnalyticsItem mapToTagAnalyticsItem(Map<String, Object> map) {
        CreatorAnalyticsVO.TagAnalyticsItem item = new CreatorAnalyticsVO.TagAnalyticsItem();
        item.setTagId(getLongValue(map, "tag_id"));
        item.setTagName((String) map.get("tag_name"));
        item.setBlogCount(getLongValue(map, "blog_count"));
        item.setAvgReads(getDoubleValue(map, "avg_reads"));
        return item;
    }

    private CreatorAnalyticsVO.HeatmapDay mapToHeatmapDay(Map<String, Object> map) {
        CreatorAnalyticsVO.HeatmapDay day = new CreatorAnalyticsVO.HeatmapDay();
        day.setDate(map.get("date").toString());
        day.setPublishCount(getLongValue(map, "publish_count"));
        return day;
    }

    private Long getLongValue(Map<String, Object> map, String key) {
        Object val = map.get(key);
        if (val == null) return 0L;
        if (val instanceof Long) return (Long) val;
        if (val instanceof Integer) return ((Integer) val).longValue();
        if (val instanceof Number) return ((Number) val).longValue();
        return 0L;
    }

    private Double getDoubleValue(Map<String, Object> map, String key) {
        Object val = map.get(key);
        if (val == null) return 0.0;
        if (val instanceof Double) return (Double) val;
        if (val instanceof Number) return ((Number) val).doubleValue();
        return 0.0;
    }
}
