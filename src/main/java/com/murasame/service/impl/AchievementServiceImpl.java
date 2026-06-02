package com.murasame.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.murasame.entity.Achievement;
import com.murasame.mapper.AchievementMapper;
import com.murasame.mapper.UserMapper;
import com.murasame.service.AchievementService;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class AchievementServiceImpl implements AchievementService {

    private static final Logger log = LoggerFactory.getLogger(AchievementServiceImpl.class);
    private static final ObjectMapper mapper = new ObjectMapper();

    @Resource
    private AchievementMapper achievementMapper;

    @Resource
    private UserMapper userMapper;

    @Override
    public List<Achievement> getAllAchievements() {
        return achievementMapper.findAll();
    }

    @Override
    public List<Integer> getUserAchievementIds(Long userId) {
        String json = userMapper.getAchievementById(userId);
        if (json == null || json.isBlank()) return new ArrayList<>();
        try {
            return mapper.readValue(json, new TypeReference<List<Integer>>() {});
        } catch (JsonProcessingException e) {
            log.warn("Failed to parse achievement JSON for user {}", userId, e);
            return new ArrayList<>();
        }
    }

    @Override
    public boolean grantAchievement(Long userId, int achievementId) {
        List<Integer> ids = getUserAchievementIds(userId);
        if (ids.contains(achievementId)) return false;  // 已拥有
        ids.add(achievementId);
        try {
            String json = mapper.writeValueAsString(ids);
            userMapper.updateAchievement(userId, json);
            return true;
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize achievement list for user {}", userId, e);
            return false;
        }
    }
}
