package com.murasame.service;

import com.murasame.domain.vo.GitHubProfileVO;

import com.murasame.domain.dto.GitHubRepoDTO;

import java.util.List;

public interface GitHubService {
    GitHubProfileVO getGitHubProfile(String username, String userToken);

    List<GitHubRepoDTO> getRepos(String username, int page, int perPage, String userToken);
}
