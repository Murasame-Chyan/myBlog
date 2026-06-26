package com.murasame.service;

import com.murasame.domain.vo.CreatorAnalyticsVO;

import java.util.List;
import java.util.Map;

public interface CreatorAnalyticsService {
    /**
     * 获取创作者数据分析
     * @param userId 创作者ID
     * @param days 近N天（可选，默认30）
     * @param startDate 自定义开始日期（可选）
     * @param endDate 自定义结束日期（可选）
     * @param tagIds 标签ID列表过滤（可选，null=全部）
     * @return 分析数据VO
     */
    CreatorAnalyticsVO getAnalytics(Long userId, Integer days, String startDate, String endDate, List<Long> tagIds);

    /**
     * 获取用户已使用的所有标签列表
     * @param userId 创作者ID
     * @return 标签列表（id + tagName）
     */
    List<Map<String, Object>> getUserTags(Long userId);
}
