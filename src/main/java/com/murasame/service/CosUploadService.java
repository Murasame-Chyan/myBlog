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
}
