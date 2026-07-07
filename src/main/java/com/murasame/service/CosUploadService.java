package com.murasame.service;

import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

public interface CosUploadService {

    /**
     * 上传博客图片
     * @param file 图片文件
     * @return 图片访问 URL
     */
    String uploadImage(MultipartFile file) throws IOException;

    /**
     * 上传用户头像
     * @param file 头像文件
     * @param userId 用户ID
     * @return 头像访问 URL
     */
    String uploadAvatar(MultipartFile file, Long userId) throws IOException;

    /**
     * 上传博客封面图片（不压缩，原图上传）
     * @param file 封面图片文件
     * @return 封面图片访问 URL
     */
    String uploadCoverImage(MultipartFile file) throws IOException;

    /**
     * 上传 base64 编码的图片（剪贴板粘贴场景自动转换）
     * 支持从网页/Word/Markdown 粘贴时自动提取内嵌图片并转存 COS
     *
     * @param dataUrl 完整 data URL，如 "data:image/png;base64,iVBOR..."
     * @return 图片访问 URL
     */
    String uploadBase64Image(String dataUrl) throws IOException;
}
