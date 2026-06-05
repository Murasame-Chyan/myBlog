package com.murasame.service;

import com.murasame.domain.vo.CreatorAnalyticsVO;

public interface CreatorAnalyticsService {
    /**
     * 获取创作者数据分析（30天趋势）
     * @param userId 创作者ID
     * @return 分析数据VO
     */
    CreatorAnalyticsVO getAnalytics(Long userId);
}
