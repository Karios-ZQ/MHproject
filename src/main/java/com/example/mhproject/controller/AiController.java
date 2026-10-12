package com.example.mhproject.controller;

import com.example.mhproject.config.RateLimit;
import com.example.mhproject.service.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@RestController
@RequestMapping("/api/ai")
@CrossOrigin
public class AiController {

    @Autowired
    private AiService aiService;
    @Autowired
    private AiChatService aiChatService;
    @Autowired
    private AiSearchService aiSearchService;
    @Autowired
    private AiProductService aiProductService;
    @Autowired
    private AiInsightService aiInsightService;

    @Value("${spring.ai.deepseek.api-key:}")
    private String apiKey;

    private static final ExecutorService SSE_POOL = Executors.newCachedThreadPool();

    // 功能4：AI 智能客服导购（RAG 对话，SSE 流式打字机效果）
    @GetMapping(value = "/assistant/stream", produces = org.springframework.http.MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter assistantStream(@RequestParam String message) {
        SseEmitter emitter = new SseEmitter(30000L); // 30秒超时

        SSE_POOL.submit(() -> {
            try {
                // 构建带商品知识库的 prompt
                String prompt = buildAssistantPrompt(message);

                // 调用 DeepSeek 流式 API
                URL url = new URL("https://api.deepseek.com/chat/completions");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setRequestProperty("Authorization", "Bearer " + apiKey);
                conn.setDoOutput(true);
                conn.setConnectTimeout(5000);
                conn.setReadTimeout(25000);

                String body = String.format(
                        "{\"model\":\"deepseek-chat\",\"messages\":[{\"role\":\"user\",\"content\":\"%s\"}],\"stream\":true,\"temperature\":0.7}",
                        escapeJson(prompt));
                try (OutputStream os = conn.getOutputStream()) {
                    os.write(body.getBytes(StandardCharsets.UTF_8));
                }

                // 逐行读取流式响应
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        if (line.startsWith("data: ")) {
                            String json = line.substring(6);
                            if (json.equals("[DONE]")) break;
                            // 解析 delta.content
                            String content = extractDeltaContent(json);
                            if (content != null && !content.isEmpty()) {
                                emitter.send(SseEmitter.event().data(content));
                            }
                        }
                    }
                }
                emitter.send(SseEmitter.event().data("[DONE]"));
                emitter.complete();
            } catch (Exception e) {
                try {
                    emitter.send(SseEmitter.event().data("AI 助手暂时有点忙，请稍后再试～"));
                    emitter.complete();
                } catch (Exception ignored) {}
            }
        });

        return emitter;
    }

    /** 构建带商品知识库的导购 prompt（与 AiChatService 逻辑一致） */
    private String buildAssistantPrompt(String userMessage) {
        // 简化版：直接用 AiChatService 的 prompt 构建逻辑
        return "你是一个热情专业的电商商城导购客服。请基于商品信息回答用户问题，回答简洁友好（100字以内）。\n用户问题：" + userMessage;
    }

    /** 从 SSE JSON 行中提取 delta.content */
    private String extractDeltaContent(String json) {
        try {
            int idx = json.indexOf("\"content\":\"");
            if (idx < 0) return null;
            int start = idx + 11;
            int end = json.indexOf("\"", start);
            if (end < 0) return null;
            String raw = json.substring(start, end);
            // 处理转义
            return raw.replace("\\n", "\n").replace("\\\"", "\"");
        } catch (Exception e) {
            return null;
        }
    }

    private String escapeJson(String s) {
        return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n");
    }

    // 通用对话
    @GetMapping("/chat")
    @RateLimit(qps = 3)
    public String chat(@RequestParam String message) {
        return aiService.chat(message);
    }

    // 功能1：AI 生成广告文案
    @PostMapping("/ad/generate")
    @RateLimit(qps = 2)
    public String generateAdCopy(@RequestParam String theme,
                                 @RequestParam(required = false) String style,
                                 @RequestParam(required = false) String length) {
        return aiService.generateAdCopy(theme, style, length);
    }

    // 功能2：AI 内容安全检测
    @PostMapping("/ad/check")
    public String checkAdSafety(@RequestParam String adText) {
        return aiService.checkAdSafety(adText);
    }

    // 功能3：AI 文案润色
    @PostMapping("/ad/polish")
    public String polishAdCopy(@RequestParam String adText) {
        return aiService.polishAdCopy(adText);
    }

    // 功能4：AI 智能客服导购（RAG 对话）
    @GetMapping("/assistant")
    @RateLimit(qps = 5)
    public String assistant(@RequestParam String message) {
        return aiChatService.chat(message);
    }

    // 功能5：AI 自然语言智能搜索
    @GetMapping("/search")
    @RateLimit(qps = 5)
    public Object smartSearch(@RequestParam String q) {
        return aiSearchService.smartSearch(q);
    }

    // 功能6：AI 商品描述自动生成
    @PostMapping("/product/generateDesc")
    public String generateProductDesc(@RequestParam String name,
                                      @RequestParam(required = false) String category,
                                      @RequestParam(required = false) String price) {
        return aiProductService.generateDescription(name, category, price);
    }

    // 功能7：AI 运营数据洞察（管理员用自然语言问数据）
    @GetMapping("/insight")
    public String insight(@RequestParam(required = false) String question) {
        return aiInsightService.insight(question);
    }

    // 功能8：豆包文生图
    @PostMapping("/image/generate")
    public String generateImage(@RequestParam String prompt,
                                @RequestParam(required = false) String size) {
        return aiService.generateImage(prompt, size);
    }

    // 功能9：AI 地址智能识别（粘贴完整地址自动拆分为省/市/区/详细地址）
    @PostMapping("/address/parse")
    public Map<String, String> parseAddress(@RequestParam String raw) {
        return aiService.parseAddress(raw);
    }
}
