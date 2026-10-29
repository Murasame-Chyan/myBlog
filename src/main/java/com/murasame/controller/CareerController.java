package com.murasame.controller;

import com.murasame.domain.dto.JobApplicationCreateDTO;
import com.murasame.domain.dto.JobApplicationEventCreateDTO;
import com.murasame.domain.dto.JobApplicationQueryDTO;
import com.murasame.domain.dto.JobApplicationStatusChangeDTO;
import com.murasame.domain.dto.JobApplicationUpdateDTO;
import com.murasame.domain.vo.JobApplicationEventVO;
import com.murasame.entity.Users;
import com.murasame.service.JobApplicationService;
import com.murasame.util.AuthHelper;
import com.murasame.util.CareerEnums;
import com.murasame.util.ReturnUtil;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 求职进度模块（MovingOn）。
 *
 * 路由说明：页面走 /career/**，接口走 /api/career/**，两套前缀不同，
 * 因此本类不使用类级 @RequestMapping，而是在方法上写完整路径（按方案第 4 条：页面与 API 合并到一个 Controller）。
 *
 * 鉴权：
 * 1. /api/career/** 已加入 SecurityConfiguration 的 authenticated 清单（URL 层兜底）；
 * 2. 每个方法内再用 AuthHelper 判空返回 401（双保险，照 ArchiveController 的做法）；
 * 3. 越权与不存在统一返回 404，不返回 403，避免暴露记录是否存在。
 */
@Controller
@Validated
@Tag(name = "求职进度", description = "个人求职投递进度记录：列表/详情/事件时间线/统计")
public class CareerController {

    @Resource
    private JobApplicationService jobApplicationService;

    @Resource
    private AuthHelper authHelper;

    // ==================== 页面 ====================

    /** 入口重定向 */
    @GetMapping("/career")
    public String careerEntry() {
        return "redirect:/career/applications";
    }

    /** 求职进度页面（未登录回首页，与 /archives 的做法一致） */
    @GetMapping("/career/applications")
    public String careerPage(HttpServletRequest request, Model model) {
        if (authHelper.getCurrentUser(request) == null) {
            return "redirect:/";
        }
        // 下拉选项由服务端注入，前端不再硬编码枚举
        model.addAttribute("statusOptions", toOptions(CareerEnums.statusLabels()));
        model.addAttribute("channelOptions", toOptions(CareerEnums.channelLabels()));
        model.addAttribute("priorityOptions", toOptions(CareerEnums.priorityLabels()));
        // 只允许手动追加 note/interview/follow_up（created/status_change 由系统写入）
        model.addAttribute("eventTypeOptions", toOptions(CareerEnums.manualEventTypeLabels()));
        return "career";
    }

    // ==================== 列表 / 统计 ====================

    @ResponseBody
    @GetMapping("/api/career/applications")
    public Map<String, Object> list(HttpServletRequest request, JobApplicationQueryDTO query) {
        Users user = authHelper.getCurrentUser(request);
        if (user == null) {
            return ReturnUtil.unauthorized("请先登录");
        }
        return ReturnUtil.success(jobApplicationService.pageList(user.getId(), query));
    }

    @ResponseBody
    @GetMapping("/api/career/stats")
    public Map<String, Object> stats(HttpServletRequest request) {
        Users user = authHelper.getCurrentUser(request);
        if (user == null) {
            return ReturnUtil.unauthorized("请先登录");
        }
        return ReturnUtil.success(jobApplicationService.stats(user.getId()));
    }

    // ==================== 详情 ====================

    @ResponseBody
    @GetMapping("/api/career/applications/{id}")
    public Map<String, Object> detail(HttpServletRequest request, @PathVariable Long id) {
        Users user = authHelper.getCurrentUser(request);
        if (user == null) {
            return ReturnUtil.unauthorized("请先登录");
        }
        var detail = jobApplicationService.detail(user.getId(), id);
        if (detail == null) {
            return ReturnUtil.notFound("记录不存在");
        }
        return ReturnUtil.success(detail);
    }

    // ==================== 新建 / 编辑 / 删除 ====================

    @ResponseBody
    @PostMapping("/api/career/applications")
    public Map<String, Object> create(HttpServletRequest request,
                                      @Valid @RequestBody JobApplicationCreateDTO dto) {
        Users user = authHelper.getCurrentUser(request);
        if (user == null) {
            return ReturnUtil.unauthorized("请先登录");
        }
        try {
            Long id = jobApplicationService.create(user.getId(), dto);
            return ReturnUtil.success("创建成功", id);
        } catch (IllegalArgumentException e) {
            return ReturnUtil.badRequest(e.getMessage());
        }
    }

    @ResponseBody
    @PutMapping("/api/career/applications/{id}")
    public Map<String, Object> update(HttpServletRequest request,
                                      @PathVariable Long id,
                                      @Valid @RequestBody JobApplicationUpdateDTO dto) {
        Users user = authHelper.getCurrentUser(request);
        if (user == null) {
            return ReturnUtil.unauthorized("请先登录");
        }
        try {
            if (!jobApplicationService.update(user.getId(), id, dto)) {
                return ReturnUtil.notFound("记录不存在");
            }
            return ReturnUtil.success("更新成功");
        } catch (IllegalArgumentException e) {
            return ReturnUtil.badRequest(e.getMessage());
        }
    }

    @ResponseBody
    @DeleteMapping("/api/career/applications/{id}")
    public Map<String, Object> delete(HttpServletRequest request, @PathVariable Long id) {
        Users user = authHelper.getCurrentUser(request);
        if (user == null) {
            return ReturnUtil.unauthorized("请先登录");
        }
        if (!jobApplicationService.softDelete(user.getId(), id)) {
            return ReturnUtil.notFound("记录不存在");
        }
        return ReturnUtil.success("已删除");
    }

    // ==================== 状态 / 事件 / 跟进 ====================

    @ResponseBody
    @PostMapping("/api/career/applications/{id}/status")
    public Map<String, Object> changeStatus(HttpServletRequest request,
                                            @PathVariable Long id,
                                            @Valid @RequestBody JobApplicationStatusChangeDTO dto) {
        Users user = authHelper.getCurrentUser(request);
        if (user == null) {
            return ReturnUtil.unauthorized("请先登录");
        }
        try {
            if (!jobApplicationService.changeStatus(user.getId(), id, dto)) {
                return ReturnUtil.notFound("记录不存在");
            }
            return ReturnUtil.success("状态已更新");
        } catch (IllegalArgumentException e) {
            return ReturnUtil.badRequest(e.getMessage());
        }
    }

    @ResponseBody
    @PostMapping("/api/career/applications/{id}/events")
    public Map<String, Object> addEvent(HttpServletRequest request,
                                        @PathVariable Long id,
                                        @Valid @RequestBody JobApplicationEventCreateDTO dto) {
        Users user = authHelper.getCurrentUser(request);
        if (user == null) {
            return ReturnUtil.unauthorized("请先登录");
        }
        try {
            if (!jobApplicationService.addEvent(user.getId(), id, dto)) {
                return ReturnUtil.notFound("记录不存在");
            }
            return ReturnUtil.success("事件已添加");
        } catch (IllegalArgumentException e) {
            return ReturnUtil.badRequest(e.getMessage());
        }
    }

    @ResponseBody
    @GetMapping("/api/career/applications/{id}/events")
    public Map<String, Object> listEvents(HttpServletRequest request, @PathVariable Long id) {
        Users user = authHelper.getCurrentUser(request);
        if (user == null) {
            return ReturnUtil.unauthorized("请先登录");
        }
        List<JobApplicationEventVO> events = jobApplicationService.listEvents(user.getId(), id);
        if (events == null) {
            return ReturnUtil.notFound("记录不存在");
        }
        return ReturnUtil.success(events);
    }

    /** 跟进 +N 天（默认 7 天），返回新的跟进日期 */
    @ResponseBody
    @PostMapping("/api/career/applications/{id}/follow-up")
    public Map<String, Object> followUp(HttpServletRequest request,
                                        @PathVariable Long id,
                                        @RequestParam(value = "days", required = false) Integer days) {
        Users user = authHelper.getCurrentUser(request);
        if (user == null) {
            return ReturnUtil.unauthorized("请先登录");
        }
        try {
            LocalDate next = jobApplicationService.followUp(user.getId(), id, days);
            if (next == null) {
                return ReturnUtil.notFound("记录不存在");
            }
            return ReturnUtil.success("已设置跟进提醒", next);
        } catch (IllegalArgumentException e) {
            return ReturnUtil.badRequest(e.getMessage());
        }
    }

    // ==================== 导出 / 回收站 ====================

    /** 导出 CSV：UTF-8（带 BOM，Excel 直接打开不乱码），仅当前用户未删除记录 */
    @GetMapping("/api/career/export.csv")
    public ResponseEntity<byte[]> exportCsv(HttpServletRequest request) {
        Users user = authHelper.getCurrentUser(request);
        if (user == null) {
            return new ResponseEntity<>(new byte[0], HttpStatus.UNAUTHORIZED);
        }
        String csv = jobApplicationService.exportCsv(user.getId());
        String bom = String.valueOf((char) 0xFEFF);
        byte[] body = (bom + csv).getBytes(StandardCharsets.UTF_8);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(new MediaType("text", "csv", StandardCharsets.UTF_8));
        headers.set(HttpHeaders.CONTENT_DISPOSITION,
                "attachment; filename=career-applications-" + LocalDate.now() + ".csv");
        return new ResponseEntity<>(body, headers, HttpStatus.OK);
    }

    /** 回收站：已软删除的记录（可恢复） */
    @ResponseBody
    @GetMapping("/api/career/applications/deleted")
    public Map<String, Object> deletedList(HttpServletRequest request) {
        Users user = authHelper.getCurrentUser(request);
        if (user == null) {
            return ReturnUtil.unauthorized("请先登录");
        }
        return ReturnUtil.success(jobApplicationService.deletedList(user.getId()));
    }

    /** 恢复软删除记录 */
    @ResponseBody
    @PostMapping("/api/career/applications/{id}/restore")
    public Map<String, Object> restore(HttpServletRequest request, @PathVariable Long id) {
        Users user = authHelper.getCurrentUser(request);
        if (user == null) {
            return ReturnUtil.unauthorized("请先登录");
        }
        if (!jobApplicationService.restore(user.getId(), id)) {
            return ReturnUtil.notFound("记录不存在");
        }
        return ReturnUtil.success("已恢复");
    }

    // ==================== 辅助 ====================

    /** Map<code,label> -> [{code,label}]，供模板下拉与前端 JS 使用 */
    private List<Map<String, String>> toOptions(Map<String, String> labels) {
        List<Map<String, String>> options = new ArrayList<>();
        for (Map.Entry<String, String> entry : labels.entrySet()) {
            Map<String, String> item = new LinkedHashMap<>();
            item.put("code", entry.getKey());
            item.put("label", entry.getValue());
            options.add(item);
        }
        return options;
    }
}
