package com.murasame.controller;

import com.murasame.domain.vo.SignInStatusVO;
import com.murasame.entity.Achievement;
import com.murasame.service.AchievementService;
import com.murasame.service.SignInService;
import com.murasame.util.AuthHelper;
import com.murasame.util.ReturnUtil;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/sign-in")
public class SignInController {

    @Resource
    private SignInService signInService;

    @Resource
    private AchievementService achievementService;

    @Resource
    private AuthHelper authHelper;

    /** 获取签到状态，未登录返回 loggedIn=false */
    @GetMapping("/status")
    public Map<String, Object> status(HttpServletRequest req) {
        var user = authHelper.getCurrentUser(req);
        if (user == null) {
            SignInStatusVO vo = new SignInStatusVO();
            vo.setLoggedIn(false);
            return ReturnUtil.success(vo);
        }
        SignInStatusVO vo = signInService.getSignInStatus(user.getId());
        return ReturnUtil.success(vo);
    }

    /** 执行签到 */
    @PostMapping
    public Map<String, Object> signIn(HttpServletRequest req) {
        var user = authHelper.getCurrentUser(req);
        if (user == null) return ReturnUtil.unauthorized("请先登录");
        try {
            var result = signInService.signIn(user.getId());
            return ReturnUtil.success(result);
        } catch (IllegalStateException e) {
            return ReturnUtil.badRequest(e.getMessage());
        }
    }

    /** 小游戏补签 */
    @PostMapping("/makeup")
    public Map<String, Object> makeup(HttpServletRequest req) {
        var user = authHelper.getCurrentUser(req);
        if (user == null) return ReturnUtil.unauthorized("请先登录");
        var result = signInService.makeupSignIn(user.getId());
        return ReturnUtil.success(result);
    }

    /** 成就目录（公开）+ 当前用户已拥有列表（需登录） */
    @GetMapping("/achievements")
    public Map<String, Object> getAchievements(HttpServletRequest req) {
        List<Achievement> all = achievementService.getAllAchievements();
        List<Integer> owned = List.of();
        var user = authHelper.getCurrentUser(req);
        if (user != null) {
            owned = achievementService.getUserAchievementIds(user.getId());
        }
        return ReturnUtil.success(Map.of("achievements", all, "owned", owned));
    }
}
