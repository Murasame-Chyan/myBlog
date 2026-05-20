package com.murasame.domain.dto;

import lombok.Data;

@Data
public class GitHubRepoDTO {
    String name;
    String description;
    String language;
    String htmlUrl;
    Integer stargazersCount;
    Integer forksCount;
}
