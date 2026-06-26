package com.murasame.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * 音乐流媒体控制器
 * 通过无扩展名 URL 提供音频文件，绕过 IDM 等下载工具的拦截
 */
@RestController
@RequestMapping("/api/music")
public class MusicController {

    @GetMapping("/stream/{filename}")
    public void stream(
            @PathVariable String filename,
            HttpServletRequest request,
            jakarta.servlet.http.HttpServletResponse response) throws IOException {

        // 校验文件名，防止路径遍历
        if (!filename.matches("[a-zA-Z0-9_-]+")) {
            response.setStatus(400);
            return;
        }
        ClassPathResource resource = new ClassPathResource("static/music/" + filename + ".mp3");

        if (!resource.exists()) {
            response.setStatus(404);
            return;
        }

        long fileSize = resource.contentLength();
        String rangeHeader = request.getHeader("Range");

        response.setContentType("audio/mpeg");
        response.setHeader(HttpHeaders.CONTENT_DISPOSITION, "inline");
        response.setHeader(HttpHeaders.ACCEPT_RANGES, "bytes");
        response.setHeader(HttpHeaders.CACHE_CONTROL, "public, max-age=86400");

        if (rangeHeader == null) {
            // 完整返回
            response.setContentLengthLong(fileSize);
            try (InputStream in = resource.getInputStream();
                 OutputStream out = response.getOutputStream()) {
                byte[] buf = new byte[8192];
                int n;
                while ((n = in.read(buf)) != -1) {
                    out.write(buf, 0, n);
                }
            }
            return;
        }

        // 处理 Range 请求（支持音频 seek）
        long start = 0, end = fileSize - 1;
        if (rangeHeader.startsWith("bytes=")) {
            String range = rangeHeader.substring(6);
            String[] parts = range.split("-", 2);
            try {
                start = parts[0].isEmpty() ? 0 : Long.parseLong(parts[0]);
                if (parts.length > 1 && !parts[1].isEmpty()) {
                    end = Long.parseLong(parts[1]);
                }
            } catch (NumberFormatException e) {
                start = 0;
            }
        }

        if (start >= fileSize) {
            response.setStatus(HttpStatus.REQUESTED_RANGE_NOT_SATISFIABLE.value());
            response.setHeader(HttpHeaders.CONTENT_RANGE, "bytes */" + fileSize);
            return;
        }
        end = Math.min(end, fileSize - 1);
        long contentLength = end - start + 1;

        response.setStatus(HttpStatus.PARTIAL_CONTENT.value());
        response.setHeader(HttpHeaders.CONTENT_RANGE, "bytes " + start + "-" + end + "/" + fileSize);
        response.setContentLengthLong(contentLength);

        try (InputStream in = resource.getInputStream()) {
            in.skipNBytes(start);
            OutputStream out = response.getOutputStream();
            byte[] buf = new byte[8192];
            long remaining = contentLength;
            int n;
            while (remaining > 0 && (n = in.read(buf, 0, (int) Math.min(buf.length, remaining))) != -1) {
                out.write(buf, 0, n);
                remaining -= n;
            }
        }
    }
}
