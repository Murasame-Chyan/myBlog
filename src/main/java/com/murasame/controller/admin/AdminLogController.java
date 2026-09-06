package com.murasame.controller.admin;

import com.murasame.mapper.OperationLogMapper;
import com.murasame.mapper.SignInMapper;
import com.murasame.security.RequirePermission;
import com.murasame.util.ReturnUtil;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/logs")
public class AdminLogController {

    private final OperationLogMapper operationLogMapper;
    private final SignInMapper signInMapper;

    public AdminLogController(OperationLogMapper operationLogMapper, SignInMapper signInMapper) {
        this.operationLogMapper = operationLogMapper;
        this.signInMapper = signInMapper;
    }

    /**
     * 操作日志分页查询
     * 支持按 module（模块）、userId（操作人）、startTime/endTime（时间范围）筛选
     */
    @GetMapping("/operation")
    @RequirePermission("log:operation")
    public Map<String, Object> operationLogs(@RequestParam(defaultValue = "1") int page,
                                             @RequestParam(defaultValue = "20") int pageSize,
                                             @RequestParam(required = false) String module,
                                             @RequestParam(required = false) Long userId,
                                             @RequestParam(required = false) String startTime,
                                             @RequestParam(required = false) String endTime) {
        int offset = (page - 1) * pageSize;
        // 时间字符串 "2026-01-01 00:00:00" 转 LocalDateTime
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        LocalDateTime start = startTime != null && !startTime.isBlank()
                ? LocalDateTime.parse(startTime, formatter) : null;
        LocalDateTime end = endTime != null && !endTime.isBlank()
                ? LocalDateTime.parse(endTime, formatter) : null;

        return ReturnUtil.success(Map.of(
                "list", operationLogMapper.findAllPage(module, userId, start, end, pageSize, offset),
                "total", operationLogMapper.count(module, userId, start, end)
        ));
    }

    /** 签到日志分页查询（可按用户筛选） */
    @GetMapping("/sign-in")
    @RequirePermission("log:sign-in")
    public Map<String, Object> signInLogs(@RequestParam(defaultValue = "1") int page,
                                          @RequestParam(defaultValue = "20") int pageSize,
                                          @RequestParam(required = false) Long userId) {
        int offset = (page - 1) * pageSize;
        return ReturnUtil.success(Map.of(
                "list", signInMapper.findAllPage(userId, pageSize, offset),
                "total", signInMapper.countAll(userId)
        ));
    }
}
