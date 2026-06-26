package com.murasame.controller;

import com.murasame.domain.vo.CreatorAnalyticsVO;
import com.murasame.entity.Users;
import com.murasame.service.CreatorAnalyticsService;
import com.murasame.util.AuthHelper;
import com.murasame.util.ReturnUtil;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/creator")
@io.swagger.v3.oas.annotations.tags.Tag(name="创作者中心", description="创作者数据分析")
public class CreatorController {

    @Resource
    private AuthHelper authHelper;

    @Resource
    private CreatorAnalyticsService analyticsService;

    /**
     * 数据分析页面
     */
    @GetMapping("/analytics")
    public String analyticsPage(HttpServletRequest request, Model model) {
        Users currentUser = authHelper.getCurrentUser(request);
        if (currentUser == null) {
            return "redirect:/";
        }

        model.addAttribute("currentUser", currentUser);
        return "creatorAnalytics";
    }

    /**
     * 获取数据分析JSON（供前端ECharts调用）
     * 可选参数：days=today-7d, startDate/endDate自定义范围, tagIds标签过滤
     */
    @GetMapping("/analytics/data")
    @ResponseBody
    public Map<String, Object> getAnalyticsData(HttpServletRequest request,
            @RequestParam(required = false) Integer days,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(required = false) List<Long> tagIds) {
        Users currentUser = authHelper.getCurrentUser(request);
        if (currentUser == null) {
            return ReturnUtil.unauthorized("请先登录");
        }

        CreatorAnalyticsVO data = analyticsService.getAnalytics(
                currentUser.getId(), days, startDate, endDate, tagIds);
        return ReturnUtil.success(data);
    }

    /**
     * 获取用户所有已使用的标签列表（供标签选择器使用）
     */
    @GetMapping("/analytics/tags")
    @ResponseBody
    public Map<String, Object> getUserTags(HttpServletRequest request) {
        Users currentUser = authHelper.getCurrentUser(request);
        if (currentUser == null) {
            return ReturnUtil.unauthorized("请先登录");
        }

        List<Map<String, Object>> tags = analyticsService.getUserTags(currentUser.getId());
        return ReturnUtil.success(tags);
    }
}
