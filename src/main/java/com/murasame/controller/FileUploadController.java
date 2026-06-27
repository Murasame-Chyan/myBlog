package com.murasame.controller;

import com.murasame.entity.Users;
import com.murasame.service.CosUploadService;
import com.murasame.util.ReturnUtil;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
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
                                           HttpServletRequest request,
                                           HttpServletResponse response) {
        Users currentUser = authHelper.getCurrentUser(request);
        if (currentUser == null) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            Map<String, Object> result = new HashMap<>();
            result.put("success", 0);
            result.put("message", "请先登录");
            return result;
        }
        // 拒绝超过 15MB 的图片，给用户友好的错误提示
        if (file.getSize() > 15 * 1024 * 1024) {
            Map<String, Object> result = new HashMap<>();
            result.put("success", 0);
            result.put("message", "图片大小不能超过 15MB，请压缩后重新上传");
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
