package com.example.mhproject.service;

import com.example.mhproject.config.WebConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.UUID;

/**
 * 本地文件存储实现：文件存到 uploads 目录。
 *
 * 分布式设计：文件元数据（文件名 -> 原始文件名映射）缓存到 Redis，
 * 多实例部署时可共享文件索引。未来切换到 MinIO/OSS 只需改这个类。
 */
@Service
public class LocalFileStorageService implements FileStorageService {

    @Autowired
    private StringRedisTemplate redis;

    private static final String META_PREFIX = "mh:filemeta:";

    @Override
    public String store(MultipartFile file, String prefix) throws IOException {
        String originalFilename = file.getOriginalFilename();
        String ext = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            ext = originalFilename.substring(originalFilename.lastIndexOf("."));
        }
        String fileName = prefix + UUID.randomUUID().toString().replace("-", "") + ext;

        File dest = new File(WebConfig.UPLOAD_DIR + fileName);
        if (!dest.getParentFile().exists()) dest.getParentFile().mkdirs();
        file.transferTo(dest);

        // 文件元数据存 Redis：多实例共享文件索引
        redis.opsForValue().set(META_PREFIX + fileName, originalFilename == null ? "" : originalFilename);

        return "/uploads/" + fileName;
    }

    @Override
    public String storeBytes(byte[] data, String fileName) throws IOException {
        File dest = new File(WebConfig.UPLOAD_DIR + fileName);
        if (!dest.getParentFile().exists()) dest.getParentFile().mkdirs();
        Files.write(dest.toPath(), data);

        redis.opsForValue().set(META_PREFIX + fileName, "ai-generated");
        return "/uploads/" + fileName;
    }
}
