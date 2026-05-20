package com.murasame.domain.vo;

import com.murasame.domain.dto.GitHubAchievementDTO;
import com.murasame.domain.dto.GitHubRepoDTO;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class GitHubProfileVO {
    List<List<Map<String, Object>>> heatmap;       // 52 weeks × 7 days, each day: {date, level, count}
    Integer totalContributions;
    List<GitHubAchievementDTO> achievements;
    List<GitHubRepoDTO> repos;
}
