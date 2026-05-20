package com.murasame.controller;

import com.murasame.entity.Users;
import com.murasame.service.CosUploadService;
import com.murasame.util.ReturnUtil;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.Map;
import com.murasame.util.AuthHelper;

@Controller
@RequestMapping("/api/upload")
@io.swagger.v3.oas.annotations.tags.Tag(name="文件上传接口", description = "图片上传相关功能")
public class FileUploadController {

    @Resource
    private AuthHelper authHelper;

    @Resource
    private CosUploadService cosUploadService;

    // 图片上传需要登录校验，避免匿名用户滥用 COS 存储
    @PostMapping("/image")
    @ResponseBody
    public Map<String, Object> uploadImage(@RequestParam("editormd-image-file") MultipartFile file,
                                           HttpServletRequest request) {
        Users currentUser = authHelper.getCurrentUser(request);
        if (currentUser == null) {
            Map<String, Object> result = new HashMap<>();
            result.put("success", 0);
            result.put("message", "请先登录");
            return result;
        }
        try {
            String imageUrl = cosUploadService.uploadImage(file);

            // Editor.md 要求的响应格式
            Map<String, Object> result = new HashMap<>();
            result.put("success", 1);
            result.put("message", "上传成功");
            result.put("url", imageUrl);
            return result;
        } catch (Exception e) {
            Map<String, Object> result = new HashMap<>();
            result.put("success", 0);
            result.put("message", "上传失败: " + e.getMessage());
            return result;
        }
    }
}
