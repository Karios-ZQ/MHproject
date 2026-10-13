package com.example.mhproject.controller;

import com.example.mhproject.entity.User;
import com.example.mhproject.repository.UserRepository;
import com.example.mhproject.service.AiService;
import com.example.mhproject.service.FileStorageService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Base64;

@RestController
@RequestMapping("/api/upload")
@CrossOrigin
public class FileUploadController {

    @Autowired
    private UserRepository userRepo;

    @Autowired
    private AiService aiService;

    @Autowired
    private FileStorageService fileStorage;

    private void checkAdmin(HttpSession session) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) throw new RuntimeException("未登录！");
        User user = userRepo.findById(userId).orElseThrow(() -> new RuntimeException("用户不存在"));
        if (!"ADMIN".equals(user.getRole())) throw new RuntimeException("无权操作！");
    }

    /**
     * 本地上传图片（通过 FileStorageService 抽象层，未来可无缝切换到 OSS/MinIO）
     */
    @PostMapping("/image")
    public String uploadImage(@RequestParam("file") MultipartFile file,
                              @RequestParam(required = false, defaultValue = "product-") String prefix,
                              HttpSession session) {
        checkAdmin(session);
        if (file.isEmpty()) throw new RuntimeException("请选择文件");

        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || !originalFilename.matches(".*\\.(jpg|jpeg|png|gif|webp)$")) {
            throw new RuntimeException("只支持 jpg/png/gif/webp 格式");
        }
        if (file.getSize() > 10 * 1024 * 1024) {
            throw new RuntimeException("文件不能超过 10MB");
        }

        try {
            return fileStorage.store(file, prefix);
        } catch (Exception e) {
            throw new RuntimeException("上传失败: " + e.getMessage());
        }
    }

    /**
     * AI 生成商品图片（豆包 Seedream）
     */
    @GetMapping("/ai-generate")
    public String aiGenerateImage(@RequestParam String prompt, HttpSession session) {
        checkAdmin(session);
        if (prompt == null || prompt.trim().isEmpty()) throw new RuntimeException("请输入关键词");

        String b64 = aiService.generateImage(prompt, "1024x1024");
        if (b64.startsWith("AI 生图")) {
            return b64;
        }

        try {
            byte[] imageBytes = Base64.getDecoder().decode(b64);
            String fileName = "ai-" + java.util.UUID.randomUUID().toString().replace("-", "") + ".png";
            return fileStorage.storeBytes(imageBytes, fileName);
        } catch (Exception e) {
            throw new RuntimeException("图片保存失败: " + e.getMessage());
        }
    }
}