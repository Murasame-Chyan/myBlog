package com.murasame.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * 更新日志页面 — 数据直接在前端模板中硬编码
 */
@Controller
public class ChangelogController {

    @GetMapping("/changelog")
    public String changelog() {
        return "changelog";
    }
}
