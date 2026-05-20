package com.murasame.controller;

import com.murasame.domain.dto.GitHubRepoDTO;
import com.murasame.domain.vo.GitHubProfileVO;
import com.murasame.service.GitHubService;
import com.murasame.service.UserService;
import com.murasame.util.ReturnUtil;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RequestMapping("/api/github")
@Controller
@Tag(name = "GitHub接口", description = "GitHub 贡献热力图、仓库、成就徽章")
public class GitHubController {

    @Resource
    private GitHubService gitHubService;

    @Resource
    private UserService userService;

    @ResponseBody
    @GetMapping("/{username}/profile")
    public Map<String, Object> getProfile(@PathVariable String username) {
        if (username == null || username.isBlank()) {
            return ReturnUtil.badRequest("GitHub 用户名不能为空");
        }
        try {
            String userToken = userService.findGithubTokenByGithubUsername(username);
            GitHubProfileVO profile = gitHubService.getGitHubProfile(username, userToken);
            return ReturnUtil.success(profile);
        } catch (IllegalArgumentException e) {
            return ReturnUtil.notFound(e.getMessage());
        } catch (IllegalStateException e) {
            return ReturnUtil.custom(503, e.getMessage());
        } catch (Exception e) {
            return ReturnUtil.error("获取 GitHub 数据失败: " + e.getMessage());
        }
    }

    @ResponseBody
    @GetMapping("/{username}/repos")
    public Map<String, Object> getRepos(@PathVariable String username,
                                         @RequestParam(defaultValue = "1") int page,
                                         @RequestParam(defaultValue = "10") int perPage) {
        if (username == null || username.isBlank()) {
            return ReturnUtil.badRequest("GitHub 用户名不能为空");
        }
        try {
            String userToken = userService.findGithubTokenByGithubUsername(username);
            List<GitHubRepoDTO> repos = gitHubService.getRepos(username, page, perPage, userToken);
            return ReturnUtil.success(repos);
        } catch (Exception e) {
            return ReturnUtil.error("获取仓库列表失败: " + e.getMessage());
        }
    }
}
