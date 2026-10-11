package com.example.mhproject;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.ai.model.deepseek.autoconfigure.DeepSeekChatAutoConfiguration;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.awt.Desktop;
import java.net.URI;

@SpringBootApplication(exclude = DeepSeekChatAutoConfiguration.class)
@EnableScheduling
public class MHprojectApplication {

    public static void main(String[] args) {
        // 关键点：关闭无头模式，允许调用系统桌面组件
        System.setProperty("java.awt.headless", "false");
        SpringApplication.run(MHprojectApplication.class, args);
    }

    // 监听项目启动完成事件（比 CommandLineRunner 更准确，确保 Tomcat 已经就绪）
    @EventListener(ApplicationReadyEvent.class)
    public void autoOpenBrowser() {
        String url = "http://localhost:8080";
        System.out.println(">>> 项目启动成功，准备自动打开浏览器: " + url);
        try {
            // 方式一：使用 Java 标准 API（大部分情况有效）
            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                Desktop.getDesktop().browse(new URI(url));
                System.out.println(">>> 浏览器打开指令已发送 (Desktop方式)");
            } else {
                // 方式二：如果系统拦截了 Desktop，强制调用 Windows 底层命令
                System.out.println(">>> 尝试使用系统底层命令打开浏览器...");
                Runtime.getRuntime().exec("rundll32 url.dll,FileProtocolHandler " + url);
            }
        } catch (Exception e) {
            System.out.println(">>> 自动打开浏览器失败，请手动访问: " + url);
            e.printStackTrace();
        }
    }
}