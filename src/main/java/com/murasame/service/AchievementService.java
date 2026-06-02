package com.murasame.service;

import com.murasame.entity.Achievement;

import java.util.List;

public interface AchievementService {

    /** 获取全部成就目录 */
    List<Achievement> getAllAchievements();

    /** 获取用户已获得的成就ID列表 */
    List<Integer> getUserAchievementIds(Long userId);

    /** 授予成就 */
    boolean grantAchievement(Long userId, int achievementId);
}
