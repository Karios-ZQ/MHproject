package com.example.mhproject.controller;

import com.example.mhproject.entity.User;
import com.example.mhproject.repository.UserRepository;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/user")
@CrossOrigin
public class UserController {

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Autowired
    private UserRepository userRepository;

    // 注册接口
    @PostMapping("/register")
    public User register(@RequestParam String username,
                         @RequestParam String password,
                         @RequestParam(required = false) String confirmPassword,
                         HttpSession session) {
        if (username == null || username.trim().isEmpty()) {
            throw new RuntimeException("用户名不能为空");
        }
        if (password == null || password.isEmpty()) {
            throw new RuntimeException("密码不能为空");
        }
        if (confirmPassword != null && !password.equals(confirmPassword)) {
            throw new RuntimeException("两次输入的密码不一致");
        }
        if (userRepository.findByUsername(username.trim()) != null) {
            throw new RuntimeException("用户名已被注册");
        }

        User user = new User();
        user.setUsername(username.trim());
        // 密码 BCrypt 加密存储，绝不保存明文
        user.setPassword(passwordEncoder.encode(password));
        user.setRole("USER"); // 默认普通用户
        user.setCreateTime(LocalDateTime.now());
        userRepository.save(user);

        // 注册成功自动登录
        session.setAttribute("userId", user.getId());
        return user;
    }

    // 登录接口（兼容历史明文密码，登录成功后自动升级为加密存储）
    @PostMapping("/login")
    public User login(@RequestParam String username, @RequestParam String password, HttpSession session) {
        User user = userRepository.findByUsername(username);
        if (user == null) {
            throw new RuntimeException("用户名或密码错误");
        }
        String stored = user.getPassword();
        boolean matched;
        if (stored != null && stored.startsWith("$2")) {
            // 已是 BCrypt 加密
            matched = passwordEncoder.matches(password, stored);
        } else {
            // 旧数据明文密码：直接比较，成功则升级为加密
            matched = stored != null && stored.equals(password);
            if (matched) {
                user.setPassword(passwordEncoder.encode(password));
                userRepository.save(user);
            }
        }
        if (!matched) {
            throw new RuntimeException("用户名或密码错误");
        }
        // 将用户ID存入 Session
        session.setAttribute("userId", user.getId());
        return user; // 返回用户信息给前端（包含角色）
    }

    // 退出登录
    @PostMapping("/logout")
    public String logout(HttpSession session) {
        session.invalidate(); // 销毁 Session
        return "退出成功";
    }

    // 获取当前登录用户
    @GetMapping("/current")
    public User getCurrentUser(HttpSession session) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) return null;
        return userRepository.findById(userId).orElse(null);
    }
}
