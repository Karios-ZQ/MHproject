package com.example.mhproject.service;

import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

/**
 * 文件存储抽象层：面向接口编程。
 *
 * 当前实现：本地磁盘存储（LocalFileStorageService）
 * 未来可切换：MinIO / 阿里云 OSS / 腾讯云 COS，只需新增一个实现类，业务代码不用改。
 *
 * 多实例部署时：文件应放到共享存储（OSS/MinIO/NFS），
 * Redis 缓存文件元数据，所有实例共享同一份文件索引。
 */
public interface FileStorageService {

    /**
     * 保存文件，返回可访问的 URL
     * @param file 上传的文件
     * @param prefix 文件名前缀（如 "product-"、"ad-"）
     * @return 可访问的 URL 路径（如 /uploads/xxx.png）
     */
    String store(MultipartFile file, String prefix) throws IOException;

    /**
     * 保存字节数组（AI 生成的图片等）
     * @param data 文件字节
     * @param fileName 文件名
     * @return 可访问的 URL 路径
     */
    String storeBytes(byte[] data, String fileName) throws IOException;
}
