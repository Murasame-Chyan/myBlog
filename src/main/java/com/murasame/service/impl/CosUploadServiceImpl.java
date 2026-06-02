package com.murasame.service.impl;

import com.murasame.config.TencentCosConfig;
import com.murasame.service.CosUploadService;
import com.qcloud.cos.COSClient;
import com.qcloud.cos.model.CannedAccessControlList;
import com.qcloud.cos.model.ObjectMetadata;
import com.qcloud.cos.model.PutObjectRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Set;
import java.util.UUID;

@Service
public class CosUploadServiceImpl implements CosUploadService {

    @Autowired
    private COSClient cosClient;

    @Autowired
    private TencentCosConfig cosConfig;

    private static final Set<String> ALLOWED_EXTENSIONS =
            Set.of("jpg", "jpeg", "png", "gif", "webp");

    @Override
    public String uploadImage(MultipartFile file) throws IOException {
        // 1. 验证文件类型
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || originalFilename.isEmpty()) {
            throw new IllegalArgumentException("文件名不能为空");
        }

        String extension = getFileExtension(originalFilename);
        if (!ALLOWED_EXTENSIONS.contains(extension.toLowerCase())) {
            throw new IllegalArgumentException("不支持的文件类型，仅支持: " + ALLOWED_EXTENSIONS);
        }

        // 校验文件内容魔术字节，防止伪装扩展名上传非图片文件
        validateImageContent(file);

        // 2. 生成唯一文件名
        String fileName = generateUniqueFileName(extension);
        String key = cosConfig.getFolder() + "/" + fileName;

        // 3. 上传到 COS
        return uploadToCos(file, key, extension);
    }

    private static final int AVATAR_MAX_SIZE = 256;

    // 封面图片专用目录，与正文内嵌图片隔离
    private static final String COVER_FOLDER_SUFFIX = "/covers";

    @Override
    public String uploadAvatar(MultipartFile file, Long userId) throws IOException {
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || originalFilename.isEmpty()) {
            throw new IllegalArgumentException("文件名不能为空");
        }

        String extension = getFileExtension(originalFilename);
        if (!ALLOWED_EXTENSIONS.contains(extension.toLowerCase())) {
            throw new IllegalArgumentException("不支持的文件类型，仅支持: " + ALLOWED_EXTENSIONS);
        }

        // 压缩后统一存为 jpg
        byte[] compressed = compressAvatar(file);
        String key = buildAvatarKey(userId, originalFilename);

        uploadBytes(compressed, key, "image/jpeg");

        return cosConfig.getBaseUrl() + "/" + key;
    }

    @Override
    public String uploadCoverImage(MultipartFile file) throws IOException {
        // 1. 验证文件名
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || originalFilename.isEmpty()) {
            throw new IllegalArgumentException("文件名不能为空");
        }

        // 2. 验证文件扩展名
        String extension = getFileExtension(originalFilename);
        if (!ALLOWED_EXTENSIONS.contains(extension.toLowerCase())) {
            throw new IllegalArgumentException("不支持的文件类型，仅支持: " + ALLOWED_EXTENSIONS);
        }

        // 3. 复用魔术字节校验，防止伪装扩展名
        validateImageContent(file);

        // 4. 生成唯一文件名并上传到 covers/ 子目录，与正文图片隔离
        String fileName = generateUniqueFileName(extension);
        String key = cosConfig.getFolder() + COVER_FOLDER_SUFFIX + "/" + fileName;
        return uploadToCos(file, key, extension);
    }

    /**
     * 构建头像存储 key：用户目录 + 时间戳前缀 + 原始文件名（sanitized）
     * 用户目录隔离跨用户冲突，时间戳避免同用户同名覆盖
     */
    private String buildAvatarKey(Long userId, String originalFilename) {
        int dotIdx = originalFilename.lastIndexOf('.');
        String baseName = dotIdx > 0 ? originalFilename.substring(0, dotIdx) : originalFilename;

        // 移除不安全字符，保留字母数字、中文、常用符号
        baseName = baseName.replaceAll("[^a-zA-Z0-9._\\-\\u4e00-\\u9fa5]", "_");
        // 折叠连续下划线
        baseName = baseName.replaceAll("_+", "_");

        String prefix = "blog/avatars/" + userId + "/" + System.currentTimeMillis() + "_";
        String suffix = ".jpg";
        int maxBaseLen = 255 - cosConfig.getBaseUrl().length() - prefix.length() - suffix.length() - 1;
        if (maxBaseLen < 1) maxBaseLen = 1;
        if (baseName.length() > maxBaseLen) {
            baseName = baseName.substring(0, maxBaseLen);
        }

        return prefix + baseName + suffix;
    }

    /**
     * 压缩头像：最大 256x256，输出 JPEG，品质 0.8
     */
    private byte[] compressAvatar(MultipartFile file) throws IOException {
        BufferedImage original = ImageIO.read(file.getInputStream());
        if (original == null) {
            throw new IllegalArgumentException("无法解析图片文件");
        }

        BufferedImage resized = resizeImage(original, AVATAR_MAX_SIZE);

        return encodeJpeg(resized, 0.8f);
    }

    private BufferedImage resizeImage(BufferedImage original, int maxSize) {
        int w = original.getWidth();
        int h = original.getHeight();

        if (w <= maxSize && h <= maxSize) {
            return original;
        }

        double ratio = (double) maxSize / Math.max(w, h);
        int newW = (int) (w * ratio);
        int newH = (int) (h * ratio);

        BufferedImage resized = new BufferedImage(newW, newH, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = resized.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g.drawImage(original, 0, 0, newW, newH, null);
        g.dispose();

        return resized;
    }

    private byte[] encodeJpeg(BufferedImage image, float quality) throws IOException {
        ImageWriter writer = ImageIO.getImageWritersByFormatName("jpg").next();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (ImageOutputStream ios = ImageIO.createImageOutputStream(baos)) {
            writer.setOutput(ios);
            ImageWriteParam param = writer.getDefaultWriteParam();
            param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
            param.setCompressionQuality(quality);
            writer.write(null, new IIOImage(image, null, null), param);
        } finally {
            writer.dispose();
        }
        return baos.toByteArray();
    }

    private void uploadBytes(byte[] data, String key, String contentType) {
        ObjectMetadata metadata = new ObjectMetadata();
        metadata.setContentLength(data.length);
        metadata.setContentType(contentType);

        PutObjectRequest request = new PutObjectRequest(
                cosConfig.getBucketName(),
                key,
                new ByteArrayInputStream(data),
                metadata
        );
        request.withCannedAcl(CannedAccessControlList.PublicRead);

        cosClient.putObject(request);
    }

    private String uploadToCos(MultipartFile file, String key, String extension) throws IOException {
        ObjectMetadata metadata = new ObjectMetadata();
        metadata.setContentLength(file.getSize());
        metadata.setContentType("image/" + extension);

        PutObjectRequest putObjectRequest = new PutObjectRequest(
                cosConfig.getBucketName(),
                key,
                file.getInputStream(),
                metadata
        );
        putObjectRequest.withCannedAcl(CannedAccessControlList.PublicRead);

        cosClient.putObject(putObjectRequest);

        return cosConfig.getBaseUrl() + "/" + key;
    }

    private String generateUniqueFileName(String extension) {
        return UUID.randomUUID().toString().replace("-", "")
                + "_" + System.currentTimeMillis()
                + "." + extension;
    }

    private String getFileExtension(String filename) {
        int lastDotIndex = filename.lastIndexOf(".");
        if (lastDotIndex == -1 || lastDotIndex == filename.length() - 1) {
            throw new IllegalArgumentException("文件名无效");
        }
        return filename.substring(lastDotIndex + 1);
    }

    // 校验文件魔术字节，防止伪装扩展名上传非图片文件
    private void validateImageContent(MultipartFile file) throws IOException {
        byte[] header = new byte[12];
        try (var in = file.getInputStream()) {
            int total = 0;
            while (total < header.length) {
                int r = in.read(header, total, header.length - total);
                if (r == -1) break;
                total += r;
            }
            if (total < 4) {
                throw new IllegalArgumentException("文件内容过短，不是有效的图片");
            }
        }
        // PNG: 89 50 4E 47
        if (header[0] == (byte) 0x89 && header[1] == 0x50 && header[2] == 0x4E && header[3] == 0x47) return;
        // JPEG: FF D8 FF
        if ((header[0] & 0xFF) == 0xFF && (header[1] & 0xFF) == 0xD8 && (header[2] & 0xFF) == 0xFF) return;
        // GIF: 47 49 46 38
        if (header[0] == 0x47 && header[1] == 0x49 && header[2] == 0x46 && header[3] == 0x38) return;
        // WebP: 52 49 46 46  ?? ?? ?? ??  57 45 42 50
        if (header[0] == 0x52 && header[1] == 0x49 && header[2] == 0x46 && header[3] == 0x46
                && header[8] == 0x57 && header[9] == 0x45 && header[10] == 0x42 && header[11] == 0x50) return;
        throw new IllegalArgumentException("文件内容不是有效的图片格式");
    }
}

